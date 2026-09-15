package ar.edu.itba.certiflow.domain.model.certificate;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record CertificateIssued(CertificateId certificateId, AssetId assetId, ValidityPeriod validity,
                                LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public CertificateId aggregateId() {
        return certificateId;
    }
}
