package ar.edu.itba.certiflow.domain.model.schema;

import java.util.UUID;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;

public record SchemaId(UUID value) implements AggregateId {

    public static SchemaId generate() {
        return new SchemaId(UUID.randomUUID());
    }
}
