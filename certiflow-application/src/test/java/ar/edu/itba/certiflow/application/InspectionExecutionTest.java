package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.schema.model.OptionAnswer;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.schema.model.YesNoAnswer;
import ar.edu.itba.certiflow.inspection.model.AttachedEvidence;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.DuplicateEvidenceException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionIncompleteException;
import ar.edu.itba.certiflow.application.support.CertiflowFixture;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectionExecutionTest {

    private final CertiflowFixture fixture = new CertiflowFixture();

    @Test
    void inspectionCannotBeClosedWhileCriteriaRemainUnanswered() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.recordAnswer.execute(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);

        InspectionIncompleteException incompleteRejection = assertThrows(InspectionIncompleteException.class,
                () -> fixture.closeInspection.execute(inspection, fixture.inspector));

        assertEquals(List.of("EXT-01", "EXT-03"), incompleteRejection.pendingCriteria());
        assertFalse(fixture.inspection(inspection).isClosed());
    }

    @Test
    void answeredCriterionStaysPendingUntilItsRequiredEvidenceIsAttached() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.recordAnswer.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector);
        fixture.recordAnswer.execute(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);
        fixture.recordAnswer.execute(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);

        assertEquals(Outcome.PENDING, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
        assertThrows(InspectionIncompleteException.class, () -> fixture.closeInspection.execute(inspection, fixture.inspector));

        fixture.attachEvidence.execute(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);

        assertEquals(Outcome.APPROVED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
    }

    @Test
    void sameEvidenceCannotBeAttachedTwiceToTheSameCriterion() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.attachEvidence.execute(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);
        int entriesAfterFirstAttachment = fixture.inspectionLog.historyOf(fixture.inspection(inspection)).size();

        assertThrows(DuplicateEvidenceException.class,
                () -> fixture.attachEvidence.execute(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector));

        assertEquals(entriesAfterFirstAttachment, fixture.inspectionLog.historyOf(fixture.inspection(inspection)).size());
    }

    @Test
    void correctingAnAnswerBeforeClosingChangesTheEvaluationAndIsAudited() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        assertEquals(Outcome.REJECTED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));

        fixture.recordAnswer.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.10"), fixture.inspector);

        assertEquals(Outcome.APPROVED, fixture.inspection(inspection).evaluate().outcomeOf(fixture.pressureCriterion));
        List<AuditEntry> pressureEntries = fixture.inspectionLog.historyOf(fixture.inspection(inspection)).stream()
                .filter(entry -> entry.action() == InspectionAudit.ANSWER_RECORDED)
                .filter(entry -> entry.detail().startsWith("EXT-01"))
                .toList();
        assertEquals(2, pressureEntries.size());
        assertEquals(fixture.inspector, pressureEntries.getLast().performedBy());
    }

    @Test
    void closedInspectionRejectsNewAnswersEvidenceAndObservations() {
        InspectionId closedInspection = fixture.closedCompliantInspection();
        int entriesAtClosure = fixture.inspectionLog.historyOf(fixture.inspection(closedInspection)).size();

        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.recordAnswer.execute(closedInspection, fixture.pressureCriterion, fixture.numericAnswer("6.50"), fixture.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.attachEvidence.execute(closedInspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.addObservation.execute(closedInspection, fixture.signageCriterion, "Late note", fixture.inspector));
        assertThrows(InspectionAlreadyClosedException.class, () -> fixture.closeInspection.execute(closedInspection, fixture.inspector));
        assertEquals(entriesAtClosure, fixture.inspectionLog.historyOf(fixture.inspection(closedInspection)).size());
    }

    @Test
    void assignmentIsTheFirstAuditedFactAndIsAttributedToWhoeverAssigned() {
        InspectionId inspection = fixture.assignedInspection();

        AuditEntry firstEntry = fixture.inspectionLog.historyOf(fixture.inspection(inspection)).getFirst();

        assertEquals(InspectionAudit.ASSIGNED, firstEntry.action());
        assertEquals(fixture.planner, firstEntry.performedBy());
        assertEquals(fixture.clock.instant(), firstEntry.occurredAt());
    }

    @Test
    void answersAndEvidenceRememberWhoGaveThemAndWhen() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.recordAnswer.execute(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector);
        fixture.clock.advanceDays(1);
        fixture.attachEvidence.execute(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);

        CriterionResponse<?> response = fixture.inspection(inspection).evaluate().responses().stream()
                .filter(candidate -> candidate.isFor(fixture.pressureCriterion))
                .findFirst()
                .orElseThrow();
        GivenAnswer<?> givenAnswer = response.given().orElseThrow();
        AttachedEvidence attachedEvidence = response.attachments().getFirst();

        assertEquals(fixture.inspector, givenAnswer.answeredBy());
        assertEquals(fixture.clock.instant().minus(Duration.ofDays(1)), givenAnswer.answeredAt());
        assertEquals(fixture.inspector, attachedEvidence.attachedBy());
        assertEquals(fixture.clock.instant(), attachedEvidence.attachedAt());
    }
}
