package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public final class InvalidPolicyVersionException extends InvalidValueException {
    public InvalidPolicyVersionException(int version) {
        super("A certification policy version starts at 1, not " + version);
    }
}
