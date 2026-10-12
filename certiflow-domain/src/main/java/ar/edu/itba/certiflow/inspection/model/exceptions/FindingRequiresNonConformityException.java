package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class FindingRequiresNonConformityException extends DomainException {

    public FindingRequiresNonConformityException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " was not observed nor rejected, so it raises no finding");
    }
}
