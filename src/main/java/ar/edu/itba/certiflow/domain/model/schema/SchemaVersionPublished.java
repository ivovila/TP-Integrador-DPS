package ar.edu.itba.certiflow.domain.model.schema;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record SchemaVersionPublished(SchemaId schemaId, int number, LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return schemaId.value().toString();
    }
}
