package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.schema.model.SchemaVersion;

public interface PublishSchemaVersionUseCase {

    SchemaVersion execute(PublishSchemaVersionCommand command);
}
