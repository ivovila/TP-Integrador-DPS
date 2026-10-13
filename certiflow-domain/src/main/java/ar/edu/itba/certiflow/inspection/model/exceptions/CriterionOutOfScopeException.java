package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class CriterionOutOfScopeException extends NotFoundException {

    public CriterionOutOfScopeException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " is outside the scope assigned to this inspection");
    }
}
