package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class CorrectiveActionNotInFindingException extends DomainException {

    public CorrectiveActionNotInFindingException(CorrectiveActionId actionId) {
        super("Corrective action " + actionId.value() + " does not belong to this finding");
    }
}
