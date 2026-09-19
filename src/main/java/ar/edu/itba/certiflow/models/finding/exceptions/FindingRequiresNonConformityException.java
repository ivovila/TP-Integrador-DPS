package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class FindingRequiresNonConformityException extends DomainException {

    public FindingRequiresNonConformityException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " was not observed nor rejected, so it raises no finding");
    }
}
