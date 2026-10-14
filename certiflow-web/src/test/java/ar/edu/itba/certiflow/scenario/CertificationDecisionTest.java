package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.certification.model.PolicyCode;
import ar.edu.itba.certiflow.certification.model.StandardIneligibilityReason;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.SectionsScope;
import ar.edu.itba.certiflow.scenario.support.CertiflowFixture;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CertificationDecisionTest extends ScenarioTest {

    @Test
    void compliantClosedInspectionIsEligible() {
        CertificationDecision decision = fixture.evaluate(fixture.closedCompliantInspection());

        assertTrue(decision.isEligible());
        assertEquals(decision, decision.ensureGranted());
    }

    @Test
    void decisionExplainsEveryReasonAtOnceWithoutIssuingAnything() {
        InspectionId partialInProgress = fixture.assignedInspection(new SectionsScope(Set.of(CertiflowFixture.SIGNAGE)));

        CertificationDecision decision = fixture.evaluate(partialInProgress);

        assertFalse(decision.isEligible());
        assertEquals(new PolicyCode("STANDARD"), decision.policyCode());
        List<Ineligibility> ineligibilities = fixture.ineligibilitiesOf(decision);
        assertEquals(List.of(StandardIneligibilityReason.INSPECTION_NOT_CLOSED, StandardIneligibilityReason.PARTIAL_INSPECTION),
                ineligibilities.stream().map(Ineligibility::reason).toList());
        assertTrue(fixture.currentWholeAssetCertificate(fixture.extinguisher.code(), LocalDate.now(fixture.clock)).isEmpty());
    }

    @Test
    void blockingFindingIsReportedWithItsExplanation() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        CertificationDecision decision = fixture.evaluate(inspection);

        List<Ineligibility> ineligibilities = fixture.ineligibilitiesOf(decision);
        assertEquals(1, ineligibilities.size());
        assertEquals(StandardIneligibilityReason.BLOCKING_FINDINGS, ineligibilities.getFirst().reason());
        assertTrue(ineligibilities.getFirst().detail().contains("EXT-01"));
    }
}
