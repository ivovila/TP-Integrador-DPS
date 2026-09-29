package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record CertificateNumber(String value) {

    public CertificateNumber {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A certificate number must not be blank");
        }
    }
}
