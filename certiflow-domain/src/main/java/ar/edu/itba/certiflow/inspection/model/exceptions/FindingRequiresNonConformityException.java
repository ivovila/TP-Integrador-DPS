package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class FindingRequiresNonConformityException extends RuleViolationException {

    public FindingRequiresNonConformityException(Criterion<?> criterion) {
        super("Criterion " + criterion.code().value() + " was not observed nor rejected, so it raises no finding");
    }
}
