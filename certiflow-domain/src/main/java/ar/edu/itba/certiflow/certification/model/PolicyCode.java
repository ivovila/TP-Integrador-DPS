package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

/** El código con el que una política de certificación queda registrada en cada decisión que toma. */
public record PolicyCode(String value) {

    public PolicyCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A certification policy needs a code");
        }
    }
}
