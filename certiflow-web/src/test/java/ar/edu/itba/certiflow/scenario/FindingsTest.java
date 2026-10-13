package ar.edu.itba.certiflow.scenario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.inspection.model.FindingAudit;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import java.util.List;
import org.junit.jupiter.api.Test;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;
import ar.edu.itba.certiflow.inspection.model.Finding;

class FindingsTest extends ScenarioTest {

    @Test
    void closingRaisesOneFindingPerNonConformityWithItsSeverityEvidenceAndAssetResponsible() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "FADED");

        List<FindingId> findings = fixture.close(inspection, fixture.inspector);

        assertEquals(2, findings.size());
        FindingId lowPressureFinding = findings.getFirst();
        assertEquals(fixture.pressureCriterion, fixture.finding(lowPressureFinding).criterion());
        assertEquals(StandardSeverity.MAJOR, fixture.finding(lowPressureFinding).severity());
        assertEquals(1, fixture.finding(lowPressureFinding).evidence().size());
        assertEquals(fixture.gaugePhoto(), fixture.finding(lowPressureFinding).evidence().getFirst().evidence());
        assertEquals(fixture.inspector, fixture.finding(lowPressureFinding).evidence().getFirst().attachedBy());
        assertEquals(fixture.assetResponsible, fixture.finding(lowPressureFinding).responsible());
        assertTrue(fixture.finding(lowPressureFinding).isOpen());

        FindingId fadedSignageFinding = findings.getLast();
        assertEquals(fixture.signageCriterion, fixture.finding(fadedSignageFinding).criterion());
        assertEquals(StandardSeverity.MINOR, fixture.finding(fadedSignageFinding).severity());
        assertEquals(fixture.assetResponsible, fixture.finding(fadedSignageFinding).responsible());
        assertTrue(fixture.finding(fadedSignageFinding).isOpen());
    }

    @Test
    void raisedFindingsAreStoredAndTheirBirthIsAudited() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        assertEquals(List.of(finding), fixture.findingRepository.findByAsset(fixture.extinguisher.code()).stream().map(Finding::id).toList());
        assertEquals(FindingAudit.RAISED, fixture.findingLog.historyOf(finding).getFirst().action());
        assertEquals(fixture.inspector, fixture.findingLog.historyOf(finding).getFirst().performedBy());
    }

    @Test
    void fullyCompliantInspectionRaisesNoFindings() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");

        List<FindingId> findings = fixture.close(inspection, fixture.inspector);

        assertTrue(findings.isEmpty());
        assertTrue(fixture.findingRepository.findByAsset(fixture.extinguisher.code()).isEmpty());
    }
}
