package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public final class SchemaVersionAlreadyCancelledException extends RuleViolationException {
    public SchemaVersionAlreadyCancelledException(int number) {
        super("Version " + number + " is already cancelled");
    }
}
