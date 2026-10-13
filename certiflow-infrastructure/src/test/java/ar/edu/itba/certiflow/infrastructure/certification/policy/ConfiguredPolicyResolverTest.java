package ar.edu.itba.certiflow.infrastructure.certification.policy;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.certiflow.catalog.model.*;
import ar.edu.itba.certiflow.certification.model.*;
import ar.edu.itba.certiflow.certification.model.exceptions.JurisdictionPolicyNotFoundException;
import ar.edu.itba.certiflow.infrastructure.certification.persistence.DecisionJson;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class ConfiguredPolicyResolverTest {
    @Test void aNewJurisdictionChangesDecisionWithoutChangingCertificationService() {
        var third = new JurisdictionCode("THIRD");
        CertificationPolicy policy = new CertificationPolicy() {
            @Override public String code() { return "THIRD-POLICY"; }
            @Override public CertificationDecision evaluate(CertificationAssessmentContext context) {
                var from = context.issuanceDate();
                return new GrantedDecision(List.of(), new ValidityPeriod(from, from.plusMonths(3).minusDays(1)), code(),
                        context.jurisdictionCode(), context.evaluatedAt(), context.result().scope());
            }
        };
        var resolver = new ConfiguredPolicyResolver(Map.of(third, policy));
        var at = Instant.parse("2026-02-28T12:00:00Z");
        var result = new InspectionResult(new InspectionId("I"), new AssetCode("A"), at, true, List.of(),
                new SchemaReference(new AssetType("Equipment"), 1), List.of(), new CertificationScope.WholeAsset());
        var decision = resolver.resolve(third).evaluate(
                new CertificationAssessmentContext(result, third, at, LocalDate.of(2026, 2, 28)));
        assertEquals("THIRD-POLICY", decision.policyCode());
        GrantedDecision grantedDecision = decision.granted().orElseThrow();
        assertEquals(LocalDate.of(2026, 5, 27), grantedDecision.validity().validUntil());
        assertThrows(JurisdictionPolicyNotFoundException.class, () -> resolver.resolve(new JurisdictionCode("MISSING")));
        assertEquals(grantedDecision, DecisionJson.read(DecisionJson.write(grantedDecision)));
    }

    @Test void registeredJurisdictionsAreListedWithThePolicyThatDecidesForThem() {
        var resolver = new ConfiguredPolicyResolver(Map.of(
                new JurisdictionCode("SOUTH"), SeverityCertificationPolicy.strict(),
                new JurisdictionCode("NORTH"), SeverityCertificationPolicy.conditional()));
        assertEquals(List.of(
                new JurisdictionPolicy(new JurisdictionCode("NORTH"), "CONDITIONAL"),
                new JurisdictionPolicy(new JurisdictionCode("SOUTH"), "STRICT")), resolver.all());
    }
}
