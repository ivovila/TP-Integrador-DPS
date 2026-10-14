package ar.edu.itba.certiflow.certification.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.certification.model.exceptions.InspectionNotClosedForCertificationException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InspectionClosureTest {

    private static final InspectionId INSPECTION = new InspectionId("I-1");

    @Test
    void aClosedInspectionGivesTheReferenceThatACertificateKeeps() {
        Instant closedAt = Instant.parse("2026-01-31T10:00:00Z");
        InspectionClosure closure = new ClosedAt(closedAt);

        assertTrue(closure.isClosed());
        assertEquals(new InspectionReference(INSPECTION, closedAt), closure.referenceOf(INSPECTION));
    }

    @Test
    void anOpenInspectionCannotBackACertificate() {
        InspectionClosure closure = new StillOpen();

        assertFalse(closure.isClosed());
        assertThrows(InspectionNotClosedForCertificationException.class, () -> closure.referenceOf(INSPECTION));
    }
}
