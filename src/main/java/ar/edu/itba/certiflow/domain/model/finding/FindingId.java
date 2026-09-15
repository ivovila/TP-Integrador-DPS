package ar.edu.itba.certiflow.domain.model.finding;

import java.util.UUID;

public record FindingId(UUID value) {

    public static FindingId generate() {
        return new FindingId(UUID.randomUUID());
    }
}
