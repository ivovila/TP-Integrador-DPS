package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.details.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.details.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.FindingAudit;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class FindingsTest {

    private final CertiflowFixture app = new CertiflowFixture();

    @Test
    void closingRaisesOneFindingPerNonConformityWithItsSeverityEvidenceAndAssetResponsible() {
        Inspection inspection = app.assignedInspection();
        app.answerAll(inspection, "5.00", YesNoAnswer.YES, "FADED");

        List<Finding> findings = app.closeInspection.execute(inspection, app.inspector);

        assertEquals(2, findings.size());
        Finding lowPressure = findings.getFirst();
        assertEquals(app.pressure, lowPressure.criterion());
        assertEquals(StandardSeverity.MAJOR, lowPressure.severity());
        assertEquals(List.of(app.gaugePhoto()), lowPressure.evidence());
        assertEquals(app.assetResponsible, lowPressure.responsible());
        assertTrue(lowPressure.blocksCertification());

        Finding fadedSignage = findings.getLast();
        assertEquals(app.signage, fadedSignage.criterion());
        assertEquals(StandardSeverity.MINOR, fadedSignage.severity());
        assertEquals(app.assetResponsible, fadedSignage.responsible());
        assertTrue(fadedSignage.isOpen());
        assertEquals(false, fadedSignage.blocksCertification());
    }

    @Test
    void raisedFindingsAreStoredAndTheirBirthIsAudited() {
        Inspection inspection = app.assignedInspection();
        Finding finding = app.majorFindingOf(inspection);

        assertEquals(List.of(finding), app.findings.findByAsset(app.extinguisher));
        assertEquals(FindingAudit.RAISED, app.auditService.entriesFor(finding).getFirst().action());
        assertEquals(app.inspector, app.auditService.entriesFor(finding).getFirst().by());
    }

    @Test
    void fullyCompliantInspectionRaisesNoFindings() {
        Inspection inspection = app.assignedInspection();
        app.answerAll(inspection, "7.00", YesNoAnswer.YES, "VISIBLE");

        List<Finding> findings = app.closeInspection.execute(inspection, app.inspector);

        assertTrue(findings.isEmpty());
        assertTrue(app.findings.findByAsset(app.extinguisher).isEmpty());
    }
}
