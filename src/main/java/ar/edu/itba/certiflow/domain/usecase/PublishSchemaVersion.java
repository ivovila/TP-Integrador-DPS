package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.SchemaRepository;

public class PublishSchemaVersion {

    private final SchemaRepository schemas;
    private final Clock clock;
    private final EventPublisher events;

    public PublishSchemaVersion(SchemaRepository schemas, Clock clock, EventPublisher events) {
        this.schemas = schemas;
        this.clock = clock;
        this.events = events;
    }

    public SchemaVersion execute(SchemaId schemaId) {
        InspectionSchema schema = schemas.getById(schemaId);
        SchemaVersion version = schema.publish(clock.now());
        schemas.save(schema);
        events.publishFrom(schema);
        return version;
    }
}
