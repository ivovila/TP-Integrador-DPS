package ar.edu.itba.certiflow.schema.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class InvalidRuleConfigurationException extends InvalidValueException {

    public InvalidRuleConfigurationException(String message) {
        super(message);
    }
}
