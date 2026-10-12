package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.Revision;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import org.junit.jupiter.api.Test;

class RectificationTest {

    private static final String REASON = "Gauge reading was transcribed incorrectly: 5.00 instead of 7.00";

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void rectificationPreservesTheOriginalRevisionAndIsAudited() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);
        fixture.clock.advanceDays(2);

        fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

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

        AuditEntry rectificationEntry = fixture.inspectionLog.historyOf(inspection).getLast();
        assertEquals(InspectionAudit.RECTIFIED, rectificationEntry.action());
        assertEquals(fixture.certifier, rectificationEntry.performedBy());
        assertTrue(rectificationEntry.detail().contains(REASON));
    }

    @Test
    void inspectionStillInProgressIsCorrectedDirectlyAndCannotBeRectified() {
        InspectionId inProgress = fixture.assignedInspection();

        assertThrows(InspectionNotClosedException.class,
                () -> fixture.rectify(inProgress, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier));
    }

    @Test
    void rectificationWithoutReasonIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.majorFindingOf(inspection);

        assertThrows(RectificationReasonRequiredException.class,
                () -> fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), " ", fixture.certifier));

        assertEquals(Outcome.REJECTED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
        assertEquals(1, fixture.inspection(inspection).revisions().size());
    }

    @Test
    void rectificationLeavesFindingsAlreadyRaisedUntouched() {
        InspectionId inspection = fixture.assignedInspection();
        FindingId finding = fixture.majorFindingOf(inspection);

        fixture.rectify(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), REASON, fixture.certifier);

        assertTrue(fixture.finding(finding).isOpen());
        assertTrue(fixture.finding(finding).blocksCertification());
    }
}
