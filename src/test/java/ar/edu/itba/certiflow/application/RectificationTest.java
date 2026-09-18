package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionAudit;
import ar.edu.itba.certiflow.domain.inspection.InspectionNotClosedException;
import ar.edu.itba.certiflow.domain.inspection.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.domain.inspection.Revision;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import org.junit.jupiter.api.Test;

class RectificationTest {

    private static final String REASON = "Gauge reading was transcribed incorrectly: 5.00 instead of 7.00";

    private final CertiflowFixture app = new CertiflowFixture();

    @Test
    void rectificationPreservesTheOriginalRevisionAndIsAudited() {
        Inspection inspection = app.assignedInspection();
        app.majorFindingOf(inspection);
        app.clock.advanceDays(2);

        app.rectifyInspection.execute(inspection, app.pressure, app.numericAnswer("7.00"), REASON, app.certifier);

        assertEquals(Outcome.APPROVED, inspection.evaluate().outcomeOf(app.pressure));
        Revision original = inspection.originalRevision();
        assertEquals(1, original.number());
        assertEquals(Outcome.REJECTED, original.evaluation().outcomeOf(app.pressure));
        assertEquals(app.inspector, original.by());

        Revision rectified = inspection.revisions().getLast();
        assertEquals(2, rectified.number());
        assertEquals(REASON, rectified.reason());
        assertEquals(app.certifier, rectified.by());
        assertEquals(app.clock.instant(), rectified.at());
        assertEquals(Outcome.APPROVED, rectified.evaluation().outcomeOf(app.pressure));

        AuditEntry entry = app.auditService.entriesFor(inspection).getLast();
        assertEquals(InspectionAudit.RECTIFIED, entry.action());
        assertEquals(app.certifier, entry.by());
        assertTrue(entry.detail().contains(REASON));
    }

    @Test
    void inspectionStillInProgressIsCorrectedDirectlyAndCannotBeRectified() {
        Inspection inProgress = app.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> app.rectifyInspection.execute(inProgress, app.pressure, app.numericAnswer("7.00"), REASON, app.certifier));
    }

    @Test
    void rectificationWithoutReasonIsRejectedAndChangesNothing() {
        Inspection inspection = app.assignedInspection();
        app.majorFindingOf(inspection);

        assertThrows(RectificationReasonRequiredException.class,
                () -> app.rectifyInspection.execute(inspection, app.pressure, app.numericAnswer("7.00"), "  ", app.certifier));

        assertEquals(Outcome.REJECTED, inspection.evaluate().outcomeOf(app.pressure));
        assertEquals(1, inspection.revisions().size());
    }

    @Test
    void rectificationLeavesFindingsAlreadyRaisedUntouched() {
        Inspection inspection = app.assignedInspection();
        Finding finding = app.majorFindingOf(inspection);

        app.rectifyInspection.execute(inspection, app.pressure, app.numericAnswer("7.00"), REASON, app.certifier);

        assertTrue(finding.isOpen());
        assertTrue(finding.blocksCertification());
    }
}
