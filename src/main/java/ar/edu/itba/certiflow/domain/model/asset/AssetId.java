package ar.edu.itba.certiflow.domain.model.asset;

import java.util.UUID;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;

public record AssetId(UUID value) implements AggregateId {

    public static AssetId generate() {
        return new AssetId(UUID.randomUUID());
    }
}
