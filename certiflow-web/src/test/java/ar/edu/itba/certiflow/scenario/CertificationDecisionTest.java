package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;

class CertificationDecisionTest extends ScenarioTest {

    @Test
    void compliantClosedInspectionIsEligible() {
        CertificationDecision decision = fixture.evaluate(fixture.closedCompliantInspection());

        assertTrue(decision.isEligible());
        assertTrue(decision.ineligibilities().isEmpty());
    }

    @Test
    void decisionExplainsEveryReasonAtOnceWithoutIssuingAnything() {
        InspectionId partialInProgress = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));

        CertificationDecision decision = fixture.evaluate(partialInProgress);

        assertFalse(decision.isEligible());
        assertEquals("STANDARD", decision.policyCode());
        assertTrue(decision.validity().isEmpty());
        assertEquals(2, decision.ineligibilities().size());
        assertTrue(decision.isBlockedBy(StandardIneligibilityReason.INSPECTION_NOT_CLOSED));
        assertTrue(decision.isBlockedBy(StandardIneligibilityReason.PARTIAL_INSPECTION));
        assertTrue(fixture.currentWholeAssetCertificate(fixture.extinguisher.code(), LocalDate.now(fixture.clock)).isEmpty());
    }

    @Test
    void blockingFindingIsReportedWithItsExplanation() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        CertificationDecision decision = fixture.evaluate(inspection);

        assertEquals(1, decision.ineligibilities().size());
        assertEquals(StandardIneligibilityReason.BLOCKING_FINDINGS, decision.ineligibilities().getFirst().reason());
        assertTrue(decision.ineligibilities().getFirst().detail().contains("EXT-01"));
    }
}
