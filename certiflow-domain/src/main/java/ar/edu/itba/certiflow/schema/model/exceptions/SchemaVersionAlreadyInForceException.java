package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import java.time.Instant;

public final class SchemaVersionAlreadyInForceException extends RuleViolationException {
    public SchemaVersionAlreadyInForceException(int number, Instant effectiveFrom) {
        super("Version " + number + " is in force since " + effectiveFrom + " and can no longer be cancelled");
    }
}
