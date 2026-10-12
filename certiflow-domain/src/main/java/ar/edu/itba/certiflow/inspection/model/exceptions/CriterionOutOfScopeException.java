package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class CriterionOutOfScopeException extends DomainException {

    public CriterionOutOfScopeException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " is outside the scope assigned to this inspection");
    }
}
