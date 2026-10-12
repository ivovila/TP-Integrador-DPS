package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class CriterionNotInSchemaException extends DomainException {

    public CriterionNotInSchemaException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " does not belong to the schema version bound to this inspection");
    }
}
