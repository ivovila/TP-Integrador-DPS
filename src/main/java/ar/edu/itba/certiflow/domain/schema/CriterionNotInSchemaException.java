package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class CriterionNotInSchemaException extends DomainException {

    public CriterionNotInSchemaException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " does not belong to the schema version bound to this inspection");
    }
}
