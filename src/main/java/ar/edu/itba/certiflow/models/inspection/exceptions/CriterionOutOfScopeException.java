package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CriterionOutOfScopeException extends DomainException {

    public CriterionOutOfScopeException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " is outside the scope assigned to this inspection");
    }
}
