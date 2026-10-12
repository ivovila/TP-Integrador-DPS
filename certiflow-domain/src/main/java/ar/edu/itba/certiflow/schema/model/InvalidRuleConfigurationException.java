package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.DomainException;

public class InvalidRuleConfigurationException extends DomainException {

    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}
