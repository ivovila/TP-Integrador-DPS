package ar.edu.itba.certiflow.models.schema.exceptions;

import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CriterionNotInSchemaException extends DomainException {

    public CriterionNotInSchemaException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " does not belong to the schema version bound to this inspection");
    }
}
