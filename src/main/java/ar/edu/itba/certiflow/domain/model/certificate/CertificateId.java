package ar.edu.itba.certiflow.domain.model.certificate;

import java.util.UUID;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;

public record CertificateId(UUID value) implements AggregateId {

    public static CertificateId generate() {
        return new CertificateId(UUID.randomUUID());
    }
}
