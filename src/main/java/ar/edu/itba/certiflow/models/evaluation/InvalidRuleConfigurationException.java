package ar.edu.itba.certiflow.models.evaluation;

import ar.edu.itba.certiflow.models.shared.DomainException;

public class InvalidRuleConfigurationException extends DomainException {

    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}
