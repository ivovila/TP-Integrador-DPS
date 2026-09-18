package ar.edu.itba.certiflow.domain.evaluation;

import ar.edu.itba.certiflow.domain.shared.DomainException;

public class InvalidRuleConfigurationException extends DomainException {

    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}
