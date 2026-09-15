package ar.edu.itba.certiflow.domain.model.asset;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record AssetRelocated(AssetId assetId, Location from, Location to, LocalDateTime occurredAt)
        implements DomainEvent {

    @Override
    public AssetId aggregateId() {
        return assetId;
    }
}
