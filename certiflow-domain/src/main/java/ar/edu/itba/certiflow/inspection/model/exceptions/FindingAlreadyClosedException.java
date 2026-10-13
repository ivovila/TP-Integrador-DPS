package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class FindingAlreadyClosedException extends RuleViolationException {

    public FindingAlreadyClosedException() {
        super("A closed finding accepts no further corrective actions or verifications");
    }
}
