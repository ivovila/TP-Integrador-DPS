package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
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
        Revisions revisions = new Revisions()
                .sealedWith("Inspection closed", EMPTY_EVALUATION, new Stamp(INSPECTOR, Instant.EPOCH))
                .sealedWith("Gauge reading transcribed incorrectly", EMPTY_EVALUATION, new Stamp(INSPECTOR, Instant.EPOCH));

        assertEquals(List.of(1, 2), revisions.asList().stream().map(Revision::number).toList());
        assertEquals(1, revisions.original().number());
        assertEquals(2, revisions.last().number());
    }

    @Test
    void sealingARevisionLeavesThePreviousOnesUntouched() {
        Revisions closure = new Revisions().sealedWith("Inspection closed", EMPTY_EVALUATION, new Stamp(INSPECTOR, Instant.EPOCH));

        Revisions rectified = closure.sealedWith("Corrected", EMPTY_EVALUATION, new Stamp(INSPECTOR, Instant.EPOCH));

        assertEquals(1, closure.asList().size());
        assertEquals(2, rectified.asList().size());
        assertEquals(closure.original(), rectified.original());
    }

    @Test
    void anInspectionWithoutRevisionsHasNoOriginalOne() {
        Revisions revisions = new Revisions();

        assertTrue(revisions.isEmpty());
        assertThrows(InspectionNotClosedException.class, revisions::original);
    }
}
