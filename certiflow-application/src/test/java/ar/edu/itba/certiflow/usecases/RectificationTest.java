package ar.edu.itba.certiflow.usecases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAudit;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
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
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);
        fixture.clock.advanceDays(2);

        fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

        assertEquals(Outcome.APPROVED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
        Revision originalRevision = fixture.inspection(inspection).originalRevision();
        assertEquals(1, originalRevision.number());
        assertEquals(Outcome.REJECTED, originalRevision.evaluation().outcomeOf(fixture.pressureCriterion));
        assertEquals(fixture.inspector, originalRevision.sealedBy());

        Revision rectifiedRevision = fixture.inspection(inspection).revisions().last();
        assertEquals(2, rectifiedRevision.number());
        assertEquals(REASON, rectifiedRevision.reason());
        assertEquals(fixture.certifier, rectifiedRevision.sealedBy());
        assertEquals(fixture.clock.instant(), rectifiedRevision.sealedAt());
        assertEquals(Outcome.APPROVED, rectifiedRevision.evaluation().outcomeOf(fixture.pressureCriterion));

        AuditEntry rectificationEntry = fixture.inspectionLog.historyOf(fixture.inspection(inspection)).getLast();
        assertEquals(InspectionAudit.RECTIFIED, rectificationEntry.action());
        assertEquals(fixture.certifier, rectificationEntry.performedBy());
        assertTrue(rectificationEntry.detail().contains(REASON));
    }

    @Test
    void inspectionStillInProgressIsCorrectedDirectlyAndCannotBeRectified() {
        InspectionId inProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> fixture.rectifyInspection.execute(inProgress, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier));
    }

    @Test
    void rectificationWithoutReasonIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        assertThrows(RectificationReasonRequiredException.class,
                () -> fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), "  ", fixture.certifier));

        assertEquals(Outcome.REJECTED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
        assertEquals(1, fixture.inspection(inspection).revisions().size());
    }

    @Test
    void rectificationLeavesFindingsAlreadyRaisedUntouched() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        fixture.rectifyInspection.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

        assertTrue(fixture.finding(finding).isOpen());
        assertTrue(fixture.finding(finding).blocksCertification());
    }
}
