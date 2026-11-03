package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record PolicyCode(String value) {

    public PolicyCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A certification policy needs a code");
        }
    }
}
