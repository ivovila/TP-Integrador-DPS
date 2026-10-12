package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record CertificateNumber(String value) {

    public CertificateNumber {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A certificate number must not be blank");
        }
    }
}
