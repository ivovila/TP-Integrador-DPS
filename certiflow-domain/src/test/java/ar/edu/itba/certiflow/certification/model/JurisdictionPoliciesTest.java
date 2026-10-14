package ar.edu.itba.certiflow.certification.model;


import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.certiflow.catalog.model.*;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class JurisdictionPoliciesTest {
    private final Instant now = Instant.parse("2026-01-31T12:00:00Z");

    private InspectionResult result(boolean closed, boolean covered, ar.edu.itba.certiflow.schema.model.Severity severity) {
        return new InspectionResult(new InspectionId("I-1"), new AssetCode("A-1"), closed ? new ClosedAt(now) : new StillOpen(),
                covered, severity == null ? List.of() : List.of(new OpenNonConformity("C-1", severity, "F-1", new NoSubsystem())),
                new SchemaReference(new AssetType("Equipment"), 1), List.of(), new WholeAssetScope());
    }

    private SeverityCertificationPolicy strictPolicy() {
        return new SeverityCertificationPolicy(new PolicyCode("STRICT"),
                Map.of(StandardSeverity.MINOR, StandardFindingTreatment.BLOCKS, StandardSeverity.MAJOR, StandardFindingTreatment.BLOCKS,
                        StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(12));
    }

    private SeverityCertificationPolicy conditionalPolicy() {
        return new SeverityCertificationPolicy(new PolicyCode("CONDITIONAL"),
                Map.of(StandardSeverity.MINOR, new ConditionalTreatment(new DaysTerm(30)),
                        StandardSeverity.MAJOR, new ConditionalTreatment(new DaysTerm(30)),
                        StandardSeverity.CRITICAL, StandardFindingTreatment.BLOCKS),
                new MonthsTerm(6));
    }

    private CertificationDecision evaluate(boolean strict, InspectionResult result) {
        var policy = strict ? strictPolicy() : conditionalPolicy();
        return policy.evaluate(
                new CertificationAssessmentContext(result, new JurisdictionCode(strict ? "DEMO-STRICT" : "DEMO-CONDITIONAL"), now, LocalDate.of(2026, 1, 31)));
    }

    @ParameterizedTest
    @CsvSource({"MINOR,REJECTED,CONDITIONAL", "MAJOR,REJECTED,CONDITIONAL", "CRITICAL,REJECTED,REJECTED"})
    void sameInspectionHasDifferentJurisdictionDecisions(StandardSeverity severity, CertificationOutcome strict, CertificationOutcome conditional) {
        assertEquals(strict, evaluate(true, result(true, true, severity)).outcome());
        assertEquals(conditional, evaluate(false, result(true, true, severity)).outcome());
    }

    @Test
    void assessmentPreservesTheAppliedPolicyAndJurisdiction() {
        var decision = evaluate(false, result(true, true, null));
        assertEquals(new PolicyCode("CONDITIONAL"), decision.policyCode());
        assertEquals(new JurisdictionCode("DEMO-CONDITIONAL"), decision.jurisdictionCode());
        assertEquals(new WholeAssetScope(), decision.scope());
    }

    @Test
    void inclusiveDurationsAndConditionsAreExplicit() {
        GrantedDecision conditional = evaluate(false, result(true, true, StandardSeverity.MAJOR)).ensureGranted();
        assertEquals(LocalDate.of(2026, 3, 1), conditional.validity().validUntil());
        assertEquals("F-1", conditional.conditions().getFirst().findingId());
        assertEquals(conditional.validity().validUntil(), conditional.conditions().getFirst().dueDate());
        assertEquals(LocalDate.of(2026, 7, 30), evaluate(false, result(true, true, null)).ensureGranted().validity().validUntil());
    }

    private enum CustomSeverity implements ar.edu.itba.certiflow.schema.model.Severity {
        ADVISORY;
        @Override public String code() { return name(); }
        @Override public String label() { return "Advisory"; }
        @Override public ar.edu.itba.certiflow.schema.model.Outcome outcomeWhenUnmet() {
            return ar.edu.itba.certiflow.schema.model.Outcome.OBSERVED;
        }
    }

    @Test
    void classifiesCustomSeverityWithoutKnowingItsImplementation() {
        CertificationPolicy policy = new CertificationPolicy() {
            @Override public PolicyCode code() { return new PolicyCode("CUSTOM"); }
            @Override public CertificationDecision evaluate(CertificationAssessmentContext context) {
                var from = context.issuanceDate();
                var until = from.plusDays(14);
                var conditions = context.result().openNonConformities().stream()
                        .map(finding -> new CertificationCondition(finding.findingId(), finding.criterionCode(),
                                "Verify advisory", until)).toList();
                return new GrantedDecision(conditions, new ValidityPeriod(from, until), code(), context.jurisdictionCode(),
                        context.evaluatedAt(), context.result().scope());
            }
        };
        var decision = policy.evaluate(new CertificationAssessmentContext(result(true, true, CustomSeverity.ADVISORY),
                new JurisdictionCode("CUSTOM"), now, LocalDate.of(2026, 1, 31)));
        assertEquals(CertificationOutcome.CONDITIONAL, decision.outcome());
        assertEquals(LocalDate.of(2026, 2, 14), decision.ensureGranted().validity().validUntil());
    }

    @Test
    void unconfiguredSeverityCannotBeCertifiedByOmission() {
        var decision = evaluate(false, result(true, true, CustomSeverity.ADVISORY));
        assertEquals(CertificationOutcome.REJECTED, decision.outcome());
        NotEligibleForCertificationException notEligible =
                assertThrows(NotEligibleForCertificationException.class, decision::ensureGranted);
        assertTrue(notEligible.decision().ineligibilities().stream()
                .anyMatch(ineligibility -> ineligibility.reason() == StandardIneligibilityReason.UNSUPPORTED_SEVERITY));
    }

}
