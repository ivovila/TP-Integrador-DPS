package ar.edu.itba.certiflow.application.asset;

import ar.edu.itba.certiflow.domain.asset.Asset;
import java.util.Objects;

public final class RegisterAsset {

    private final AssetRepository assets;

    public RegisterAsset(AssetRepository assets) {
        this.assets = assets;
    }

    public Asset execute(Asset asset) {
        assets.findByCode(asset.code()).ifPresent(existing -> {
            throw new AssetAlreadyRegisteredException(existing.code());
        });
        assets.save(asset);
        return asset;
    }
}
