package ar.edu.itba.certiflow.shared.model;

import java.time.Instant;

public final class StoredInstant {
    private StoredInstant() { }

    public static Instant normalize(Instant instant) {
        return Instant.ofEpochSecond(instant.getEpochSecond(),
                Math.round(instant.getNano() / 1000.0) * 1000);
    }
}
