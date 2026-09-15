package ar.edu.itba.certiflow.domain.model.certificate;

import java.util.UUID;

public record CertificateId(UUID value) {

    public static CertificateId generate() {
        return new CertificateId(UUID.randomUUID());
    }
}
