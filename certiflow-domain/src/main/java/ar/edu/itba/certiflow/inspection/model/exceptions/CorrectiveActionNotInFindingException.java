package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class CorrectiveActionNotInFindingException extends DomainException {

    public CorrectiveActionNotInFindingException(CorrectiveActionId actionId) {
        super("Corrective action " + actionId.value() + " does not belong to this finding");
    }
}
