package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;
import java.time.Instant;

public final class NoEffectiveSchemaVersionException extends DomainException {
    public NoEffectiveSchemaVersionException(Instant at) {
        super("No schema version was effective at " + at);
    }
}
