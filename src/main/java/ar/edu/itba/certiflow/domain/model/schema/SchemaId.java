package ar.edu.itba.certiflow.domain.model.schema;

import java.util.UUID;

public record SchemaId(UUID value) {

    public static SchemaId generate() {
        return new SchemaId(UUID.randomUUID());
    }
}
