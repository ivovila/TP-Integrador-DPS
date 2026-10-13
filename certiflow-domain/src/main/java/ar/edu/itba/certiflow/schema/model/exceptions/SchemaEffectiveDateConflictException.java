package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public final class SchemaEffectiveDateConflictException extends RuleViolationException {
    public SchemaEffectiveDateConflictException() {
        super("Effective dates must increase strictly in publication order");
    }
}
