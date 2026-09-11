package ar.edu.itba.certiflow.domain.asset;

import java.util.UUID;

public record AssetId(UUID value) {

    public static AssetId generate() {
        return new AssetId(UUID.randomUUID());
    }
}
