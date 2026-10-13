package ar.edu.itba.certiflow.scenario;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.exceptions.AnswerTypeMismatchException;
import ar.edu.itba.certiflow.schema.model.RawAnswer;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationCommand;
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
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.DuplicateEvidenceException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionIncompleteException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import ar.edu.itba.certiflow.scenario.support.ScenarioTest;

class InspectionExecutionTest extends ScenarioTest {

    @Test
    void inspectionCannotBeClosedWhileCriteriaRemainUnanswered() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answer(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);

        InspectionIncompleteException incompleteRejection = assertThrows(InspectionIncompleteException.class,
                () -> fixture.close(inspection, fixture.inspector));

        assertEquals(List.of(new CriterionCode("EXT-01"), new CriterionCode("EXT-03")), incompleteRejection.pendingCriteria());
        assertFalse(fixture.inspection(inspection).isClosed());
    }

    @Test
    void answeredCriterionStaysPendingUntilItsRequiredEvidenceIsAttached() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answer(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector);
        fixture.answer(inspection, fixture.sealIntactCriterion, YesNoAnswer.YES, fixture.inspector);
        fixture.answer(inspection, fixture.signageCriterion, new OptionAnswer("VISIBLE"), fixture.inspector);

        assertEquals(Outcome.PENDING, fixture.outcomeIn(fixture.inspection(inspection).evaluate(), fixture.pressureCriterion));
        assertThrows(InspectionIncompleteException.class, () -> fixture.close(inspection, fixture.inspector));

        fixture.attach(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);

        assertEquals(Outcome.APPROVED, fixture.outcomeIn(fixture.inspection(inspection).evaluate(), fixture.pressureCriterion));
    }

    @Test
    void sameEvidenceCannotBeAttachedTwiceToTheSameCriterion() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.attach(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);
        int entriesAfterFirstAttachment = fixture.inspectionLog.historyOf(inspection).size();

        assertThrows(DuplicateEvidenceException.class,
                () -> fixture.attach(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector));

        assertEquals(entriesAfterFirstAttachment, fixture.inspectionLog.historyOf(inspection).size());
    }

    @Test
    void correctingAnAnswerBeforeClosingChangesTheEvaluationAndIsAudited() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        assertEquals(Outcome.REJECTED, fixture.outcomeIn(fixture.inspection(inspection).evaluate(), fixture.pressureCriterion));

        fixture.answer(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.10"), fixture.inspector);

        assertEquals(Outcome.APPROVED, fixture.outcomeIn(fixture.inspection(inspection).evaluate(), fixture.pressureCriterion));
        List<AuditEntry> pressureEntries = fixture.inspectionLog.historyOf(inspection).stream()
                .filter(entry -> entry.action() == InspectionAudit.ANSWER_RECORDED)
                .filter(entry -> entry.detail().startsWith("EXT-01"))
                .toList();
        assertEquals(2, pressureEntries.size());
        assertEquals(fixture.inspector, pressureEntries.getLast().performedBy());
    }

    @Test
    void closedInspectionRejectsNewAnswersEvidenceAndObservations() {
        InspectionId closedInspection = fixture.closedCompliantInspection();
        int entriesAtClosure = fixture.inspectionLog.historyOf(closedInspection).size();

        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.answer(closedInspection, fixture.pressureCriterion, fixture.numericAnswer("6.50"), fixture.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.attach(closedInspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> fixture.addObservation.execute(new AddObservationCommand(closedInspection, fixture.signageCriterion.code(), "Late note", fixture.inspector)));
        assertThrows(InspectionAlreadyClosedException.class, () -> fixture.close(closedInspection, fixture.inspector));
        assertEquals(entriesAtClosure, fixture.inspectionLog.historyOf(closedInspection).size());
    }

    @Test
    void assignmentIsTheFirstAuditedFactAndIsAttributedToWhoeverAssigned() {
        InspectionId inspection = fixture.assignedInspection();

        AuditEntry firstEntry = fixture.inspectionLog.historyOf(inspection).getFirst();

        assertEquals(InspectionAudit.ASSIGNED, firstEntry.action());
        assertEquals(fixture.planner, firstEntry.performedBy());
        assertEquals(fixture.clock.instant(), firstEntry.occurredAt());
    }

    @Test
    void answersAndEvidenceRememberWhoGaveThemAndWhen() {
        InspectionId inspection = fixture.assignedInspection();
        fixture.answer(inspection, fixture.pressureCriterion, fixture.numericAnswer("7.00"), fixture.inspector);
        fixture.clock.advanceDays(1);
        fixture.attach(inspection, fixture.pressureCriterion, fixture.gaugePhoto(), fixture.inspector);

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

    @Test
    void answeringAnUnknownCriterionCodeIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();
        int entriesBefore = fixture.inspectionLog.historyOf(inspection).size();

        assertThrows(CriterionNotInSchemaException.class, () -> fixture.recordAnswer.execute(
                new RecordAnswerCommand(inspection, new CriterionCode("EXT-99"), new RawAnswer("true"), fixture.inspector)));

        assertEquals(entriesBefore, fixture.inspectionLog.historyOf(inspection).size());
    }

    @Test
    void answeringWithTheWrongTypeOfValueIsRejectedAndChangesNothing() {
        InspectionId inspection = fixture.assignedInspection();

        assertThrows(AnswerTypeMismatchException.class, () -> fixture.recordAnswer.execute(
                new RecordAnswerCommand(inspection, new CriterionCode("EXT-01"), new RawAnswer("true"), fixture.inspector)));

        assertEquals(Outcome.PENDING, fixture.outcomeIn(fixture.inspection(inspection).evaluate(), fixture.pressureCriterion));
    }
}
