package ar.edu.itba.certiflow.domain.finding.exceptions;

import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class FindingRequiresNonConformityException extends DomainException {

    public FindingRequiresNonConformityException(Criterion<?> criterion) {
        super("Criterion " + criterion.code() + " was not observed nor rejected, so it raises no finding");
    }
}
