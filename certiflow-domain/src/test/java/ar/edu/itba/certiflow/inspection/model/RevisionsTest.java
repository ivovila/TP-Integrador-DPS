package ar.edu.itba.certiflow.inspection.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RevisionsTest {

    private static final Evaluation EMPTY_EVALUATION = new Evaluation(List.of());
    private static final Person INSPECTOR = new Person(new PersonId("EMP-001"), "Ines Inspector");

    @Test
    void revisionsAreNumberedFromOneInTheOrderTheyWereSealed() {
        Revisions revisions = Revisions.none()
                .sealedWith("Inspection closed", EMPTY_EVALUATION, INSPECTOR, Instant.EPOCH)
                .sealedWith("Gauge reading transcribed incorrectly", EMPTY_EVALUATION, INSPECTOR, Instant.EPOCH);

        assertEquals(List.of(1, 2), revisions.asList().stream().map(Revision::number).toList());
        assertEquals(1, revisions.original().number());
        assertEquals(2, revisions.last().number());
    }

    @Test
    void sealingARevisionLeavesThePreviousOnesUntouched() {
        Revisions closure = Revisions.none().sealedWith("Inspection closed", EMPTY_EVALUATION, INSPECTOR, Instant.EPOCH);

        Revisions rectified = closure.sealedWith("Corrected", EMPTY_EVALUATION, INSPECTOR, Instant.EPOCH);

        assertEquals(1, closure.size());
        assertEquals(2, rectified.size());
        assertEquals(closure.original(), rectified.original());
    }

    @Test
    void anInspectionWithoutRevisionsHasNoOriginalOne() {
        Revisions revisions = Revisions.none();

        assertTrue(revisions.isEmpty());
        assertThrows(InspectionNotClosedException.class, revisions::original);
    }
}
