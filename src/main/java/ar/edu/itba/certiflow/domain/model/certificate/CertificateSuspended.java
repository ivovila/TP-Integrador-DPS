package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record CertificateSuspended(CertificateId certificateId, String reason, LocalDateTime occurredAt)
        implements DomainEvent {

    @Override
    public CertificateId aggregateId() {
        return certificateId;
    }
}
