package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class CorrectiveActionNotInFindingException extends NotFoundException {

    public CorrectiveActionNotInFindingException(CorrectiveActionId actionId) {
        super("Corrective action " + actionId.value() + " does not belong to this finding");
    }
}
