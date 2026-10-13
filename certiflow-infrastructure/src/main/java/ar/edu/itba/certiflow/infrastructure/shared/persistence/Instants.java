package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import java.time.Instant;

public final class Instants {

    private Instants() {
    }

    public static Instant asStored(Instant instant) {
        return instant == null ? null : Instant.ofEpochSecond(instant.getEpochSecond()).plusNanos(Math.round(instant.getNano() / 1000.0) * 1000);
    }
}
