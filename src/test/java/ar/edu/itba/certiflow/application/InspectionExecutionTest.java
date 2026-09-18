package ar.edu.itba.certiflow.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.details.evaluation.OptionAnswer;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.details.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.domain.inspection.InspectionAudit;
import ar.edu.itba.certiflow.domain.inspection.exceptions.InspectionIncompleteException;
import ar.edu.itba.certiflow.support.CertiflowFixture;
import java.util.List;
import org.junit.jupiter.api.Test;

class InspectionExecutionTest {

    private final CertiflowFixture app = new CertiflowFixture();

    @Test
    void inspectionCannotBeClosedWhileCriteriaRemainUnanswered() {
        Inspection inspection = app.assignedInspection();
        app.recordAnswer.execute(inspection, app.sealIntact, YesNoAnswer.YES, app.inspector);

        InspectionIncompleteException rejection = assertThrows(InspectionIncompleteException.class,
                () -> app.closeInspection.execute(inspection, app.inspector));

        assertEquals(List.of("EXT-01", "EXT-03"), rejection.pendingCriteria());
        assertFalse(inspection.isClosed());
    }

    @Test
    void answeredCriterionStaysPendingUntilItsRequiredEvidenceIsAttached() {
        Inspection inspection = app.assignedInspection();
        app.recordAnswer.execute(inspection, app.pressure, app.numericAnswer("7.00"), app.inspector);
        app.recordAnswer.execute(inspection, app.sealIntact, YesNoAnswer.YES, app.inspector);
        app.recordAnswer.execute(inspection, app.signage, new OptionAnswer("VISIBLE"), app.inspector);

        assertEquals(Outcome.PENDING, inspection.evaluate().outcomeOf(app.pressure));
        assertThrows(InspectionIncompleteException.class, () -> app.closeInspection.execute(inspection, app.inspector));

        app.attachEvidence.execute(inspection, app.pressure, app.gaugePhoto(), app.inspector);

        assertEquals(Outcome.APPROVED, inspection.evaluate().outcomeOf(app.pressure));
    }

    @Test
    void correctingAnAnswerBeforeClosingChangesTheEvaluationAndIsAudited() {
        Inspection inspection = app.assignedInspection();
        app.answerAll(inspection, "5.00", YesNoAnswer.YES, "VISIBLE");
        assertEquals(Outcome.REJECTED, inspection.evaluate().outcomeOf(app.pressure));

        app.recordAnswer.execute(inspection, app.pressure, app.numericAnswer("7.10"), app.inspector);

        assertEquals(Outcome.APPROVED, inspection.evaluate().outcomeOf(app.pressure));
        List<AuditEntry> pressureEntries = app.auditService.entriesFor(inspection).stream()
                .filter(entry -> entry.action() == InspectionAudit.ANSWER_RECORDED)
                .filter(entry -> entry.detail().startsWith("EXT-01"))
                .toList();
        assertEquals(2, pressureEntries.size());
        assertEquals(app.inspector, pressureEntries.getLast().by());
    }

    @Test
    void closedInspectionRejectsNewAnswersEvidenceAndObservations() {
        Inspection closed = app.closedCompliantInspection();
        int entriesAtClosure = app.auditService.entriesFor(closed).size();

        assertThrows(InspectionAlreadyClosedException.class,
                () -> app.recordAnswer.execute(closed, app.pressure, app.numericAnswer("6.50"), app.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> app.attachEvidence.execute(closed, app.pressure, app.gaugePhoto(), app.inspector));
        assertThrows(InspectionAlreadyClosedException.class,
                () -> app.addObservation.execute(closed, app.signage, "Late note", app.inspector));
        assertThrows(InspectionAlreadyClosedException.class, () -> app.closeInspection.execute(closed, app.inspector));
        assertEquals(entriesAtClosure, app.auditService.entriesFor(closed).size());
    }

    @Test
    void assignmentIsTheFirstAuditedFactAndIsAttributedToWhoeverAssigned() {
        Inspection inspection = app.assignedInspection();

        AuditEntry first = app.auditService.entriesFor(inspection).getFirst();

        assertEquals(InspectionAudit.ASSIGNED, first.action());
        assertEquals(app.planner, first.by());
        assertEquals(app.clock.instant(), first.at());
    }
}
