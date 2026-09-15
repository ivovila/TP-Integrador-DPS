package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record CertificateStatusChanged(CertificateId certificateId, CertificateStatus from, CertificateStatus to,
                                       String reason, LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return certificateId.value().toString();
    }
}
