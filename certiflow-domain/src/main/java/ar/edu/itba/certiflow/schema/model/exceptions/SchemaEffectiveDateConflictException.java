package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;

public final class SchemaEffectiveDateConflictException extends DomainException {
    public SchemaEffectiveDateConflictException() {
        super("Effective dates must increase strictly in publication order");
    }
}
