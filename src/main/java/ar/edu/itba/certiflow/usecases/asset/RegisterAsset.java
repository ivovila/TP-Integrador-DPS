package ar.edu.itba.certiflow.usecases.asset;

import ar.edu.itba.certiflow.application.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.ports.AssetRepository;

public final class RegisterAsset {

    private final AssetRepository assetRepository;

    public RegisterAsset(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Asset execute(Asset asset) {
        assetRepository.findByCode(asset.code()).ifPresent(existingAsset -> {
            throw new AssetAlreadyRegisteredException(existingAsset.code());
        });
        assetRepository.save(asset);
        return asset;
    }
}
