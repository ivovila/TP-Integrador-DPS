package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingAudit;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingsTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void closingRaisesOneFindingPerNonConformityWithItsSeverityEvidenceAndAssetResponsible() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "FADED");

        List<FindingId> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        assertEquals(2, findings.size());
        FindingId lowPressureFinding = findings.getFirst();
        assertEquals(fixture.pressureCriterion, fixture.finding(lowPressureFinding).criterion());
        assertEquals(StandardSeverity.MAJOR, fixture.finding(lowPressureFinding).severity());
        assertEquals(1, fixture.finding(lowPressureFinding).evidence().size());
        assertEquals(fixture.gaugePhoto(), fixture.finding(lowPressureFinding).evidence().getFirst().evidence());
        assertEquals(fixture.inspector, fixture.finding(lowPressureFinding).evidence().getFirst().attachedBy());
        assertEquals(fixture.assetResponsible, fixture.finding(lowPressureFinding).responsible());
        assertTrue(fixture.finding(lowPressureFinding).blocksCertification());

        FindingId fadedSignageFinding = findings.getLast();
        assertEquals(fixture.signageCriterion, fixture.finding(fadedSignageFinding).criterion());
        assertEquals(StandardSeverity.MINOR, fixture.finding(fadedSignageFinding).severity());
        assertEquals(fixture.assetResponsible, fixture.finding(fadedSignageFinding).responsible());
        assertTrue(fixture.finding(fadedSignageFinding).isOpen());
        assertEquals(false, fixture.finding(fadedSignageFinding).blocksCertification());
    }

    @Test
    void raisedFindingsAreStoredAndTheirBirthIsAudited() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        assertEquals(List.of(fixture.finding(finding)), fixture.findingRepository.findByAsset(fixture.extinguisher.code()));
        assertEquals(FindingAudit.RAISED, fixture.findingLog.historyOf(fixture.finding(finding)).getFirst().action());
        assertEquals(fixture.inspector, fixture.findingLog.historyOf(fixture.finding(finding)).getFirst().performedBy());
    }

    @Test
    void fullyCompliantInspectionRaisesNoFindings() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");

        List<FindingId> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        assertTrue(findings.isEmpty());
        assertTrue(fixture.findingRepository.findByAsset(fixture.extinguisher.code()).isEmpty());
    }
}
