package ar.edu.itba.certiflow.domain.model.finding;

import java.util.UUID;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;

public record FindingId(UUID value) implements AggregateId {

    public static FindingId generate() {
        return new FindingId(UUID.randomUUID());
    }
}
