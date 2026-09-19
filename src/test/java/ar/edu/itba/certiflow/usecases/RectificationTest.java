package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAudit;
import ar.edu.itba.certiflow.models.inspection.Revision;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.models.inspection.exceptions.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import org.junit.jupiter.api.Test;

class RectificationTest {

    private static final String REASON = "Gauge reading was transcribed incorrectly: 5.00 instead of 7.00";

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void rectificationPreservesTheOriginalRevisionAndIsAudited() {
        Inspection inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);
        fixture.clock.advanceDays(2);

        fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

        assertEquals(Outcome.APPROVED, inspection.evaluate().outcomeOf(fixture.pressureCriterion));
        Revision originalRevision = inspection.originalRevision();
        assertEquals(1, originalRevision.number());
        assertEquals(Outcome.REJECTED, originalRevision.evaluation().outcomeOf(fixture.pressureCriterion));
        assertEquals(fixture.inspector, originalRevision.sealedBy());

        Revision rectifiedRevision = inspection.revisions().getLast();
        assertEquals(2, rectifiedRevision.number());
        assertEquals(REASON, rectifiedRevision.reason());
        assertEquals(fixture.certifier, rectifiedRevision.sealedBy());
        assertEquals(fixture.clock.instant(), rectifiedRevision.sealedAt());
        assertEquals(Outcome.APPROVED, rectifiedRevision.evaluation().outcomeOf(fixture.pressureCriterion));

        AuditEntry rectificationEntry = fixture.auditService.entriesFor(inspection).getLast();
        assertEquals(InspectionAudit.RECTIFIED, rectificationEntry.action());
        assertEquals(fixture.certifier, rectificationEntry.performedBy());
        assertTrue(rectificationEntry.detail().contains(REASON));
    }

    @Test
    void inspectionStillInProgressIsCorrectedDirectlyAndCannotBeRectified() {
        Inspection inProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> fixture.rectifyInspection.execute(inProgress, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier));
    }

    @Test
    void rectificationWithoutReasonIsRejectedAndChangesNothing() {
        Inspection inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        assertThrows(RectificationReasonRequiredException.class,
                () -> fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), "  ", fixture.certifier));

        assertEquals(Outcome.REJECTED, inspection.evaluate().outcomeOf(fixture.pressureCriterion));
        assertEquals(1, inspection.revisions().size());
    }

    @Test
    void rectificationLeavesFindingsAlreadyRaisedUntouched() {
        Inspection inspection = fixture.assignedInspection();
        Finding finding = fixture.majorFindingOf(inspection);

        fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

        assertTrue(finding.isOpen());
        assertTrue(finding.blocksCertification());
    }
}
