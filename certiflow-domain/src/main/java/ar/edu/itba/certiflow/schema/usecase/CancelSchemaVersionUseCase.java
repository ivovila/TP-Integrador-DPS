package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.schema.model.SchemaVersion;

public interface CancelSchemaVersionUseCase {

    SchemaVersion execute(CancelSchemaVersionCommand command);
}
