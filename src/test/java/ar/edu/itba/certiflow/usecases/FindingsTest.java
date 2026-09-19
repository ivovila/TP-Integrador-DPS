package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingAudit;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingsTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void closingRaisesOneFindingPerNonConformityWithItsSeverityEvidenceAndAssetResponsible() {
        Inspection inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "FADED");

        List<Finding> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        assertEquals(2, findings.size());
        Finding lowPressureFinding = findings.getFirst();
        assertEquals(fixture.pressureCriterion, lowPressureFinding.criterion());
        assertEquals(StandardSeverity.MAJOR, lowPressureFinding.severity());
        assertEquals(1, lowPressureFinding.evidence().size());
        assertEquals(fixture.gaugePhoto(), lowPressureFinding.evidence().getFirst().evidence());
        assertEquals(fixture.inspector, lowPressureFinding.evidence().getFirst().attachedBy());
        assertEquals(fixture.assetResponsible, lowPressureFinding.responsible());
        assertTrue(lowPressureFinding.blocksCertification());

        Finding fadedSignageFinding = findings.getLast();
        assertEquals(fixture.signageCriterion, fadedSignageFinding.criterion());
        assertEquals(StandardSeverity.MINOR, fadedSignageFinding.severity());
        assertEquals(fixture.assetResponsible, fadedSignageFinding.responsible());
        assertTrue(fadedSignageFinding.isOpen());
        assertEquals(false, fadedSignageFinding.blocksCertification());
    }

    @Test
    void raisedFindingsAreStoredAndTheirBirthIsAudited() {
        Inspection inspection = fixture.assignedInspection();
        Finding finding = fixture.majorFindingOf(inspection);

        assertEquals(List.of(finding), fixture.findingRepository.findByAsset(fixture.extinguisher));
        assertEquals(FindingAudit.RAISED, fixture.findingLog.historyOf(finding).getFirst().action());
        assertEquals(fixture.inspector, fixture.findingLog.historyOf(finding).getFirst().performedBy());
    }

    @Test
    void fullyCompliantInspectionRaisesNoFindings() {
        Inspection inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");

        List<Finding> findings = fixture.closeInspection.execute(inspection, fixture.inspector);

        assertTrue(findings.isEmpty());
        assertTrue(fixture.findingRepository.findByAsset(fixture.extinguisher).isEmpty());
    }
}
