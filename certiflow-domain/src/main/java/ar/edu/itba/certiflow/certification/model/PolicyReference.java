package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.InvalidPolicyVersionException;

public record PolicyReference(PolicyCode code, int version) {

    public PolicyReference {
        if (version < 1) {
            throw new InvalidPolicyVersionException(version);
        }
    }

    public String label() {
        return code.value() + " v" + version;
    }
}
