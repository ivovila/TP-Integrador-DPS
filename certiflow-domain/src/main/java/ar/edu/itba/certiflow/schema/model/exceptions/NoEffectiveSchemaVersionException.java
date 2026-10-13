package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.NotFoundException;
import java.time.Instant;

public final class NoEffectiveSchemaVersionException extends NotFoundException {
    public NoEffectiveSchemaVersionException(Instant instant) {
        super("No schema version was effective at " + instant);
    }
}
