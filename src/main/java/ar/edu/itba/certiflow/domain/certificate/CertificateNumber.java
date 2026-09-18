package ar.edu.itba.certiflow.domain.certificate;

import java.util.Objects;

/** Identificador de negocio: el número con el que la entidad emite y reconoce el certificado. */
public record CertificateNumber(String value) {

    public CertificateNumber {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("A certificate number must not be blank");
        }
    }
}
