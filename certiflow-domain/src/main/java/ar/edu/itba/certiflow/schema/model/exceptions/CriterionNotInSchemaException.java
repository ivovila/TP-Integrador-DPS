package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class CriterionNotInSchemaException extends NotFoundException {

    public CriterionNotInSchemaException(Criterion<?> criterion) {
        this(criterion.code());
    }

    public CriterionNotInSchemaException(CriterionCode criterionCode) {
        super("Criterion " + criterionCode.value() + " does not belong to the schema version bound to this inspection");
    }
}
