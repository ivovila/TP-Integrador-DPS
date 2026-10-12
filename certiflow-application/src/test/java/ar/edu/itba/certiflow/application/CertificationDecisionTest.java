package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.IneligibilityReason;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CertificationDecisionTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void compliantClosedInspectionIsEligible() {
        CertificationDecision decision = fixture.evaluateCertification.execute(fixture.closedCompliantInspection());

        assertTrue(decision.isEligible());
        assertTrue(decision.ineligibilities().isEmpty());
    }

    @Test
    void decisionExplainsEveryReasonAtOnceWithoutIssuingAnything() {
        InspectionId partialInProgress = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));

        CertificationDecision decision = fixture.evaluateCertification.execute(partialInProgress);

        assertFalse(decision.isEligible());
        assertEquals(2, decision.ineligibilities().size());
        assertTrue(decision.isBlockedBy(IneligibilityReason.INSPECTION_NOT_CLOSED));
        assertTrue(decision.isBlockedBy(IneligibilityReason.PARTIAL_INSPECTION));
        assertTrue(fixture.certificateRepository.findCurrentFor(fixture.extinguisher.code(), LocalDate.now(fixture.clock)).isEmpty());
    }

    @Test
    void blockingFindingIsReportedWithItsExplanation() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        CertificationDecision decision = fixture.evaluateCertification.execute(inspection);

        assertEquals(1, decision.ineligibilities().size());
        assertEquals(IneligibilityReason.BLOCKING_FINDINGS, decision.ineligibilities().getFirst().reason());
        assertTrue(decision.ineligibilities().getFirst().detail().contains("EXT-0042"));
    }
}
