package ar.edu.itba.certiflow.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class StoredInstantTest {

    @Test
    void aStoredInstantNeverLandsAfterTheRealOne() {
        Instant real = Instant.parse("2026-10-13T15:52:46.849756900Z");

        assertEquals(Instant.parse("2026-10-13T15:52:46.849756Z"), StoredInstant.normalize(real));
    }
}
