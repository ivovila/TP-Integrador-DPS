package ar.edu.itba.certiflow.application.asset;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.usecases.asset.RegisterAsset;

public final class RegisterAssetInteractor implements RegisterAsset {

    private final AssetRepository assetRepository;

    public RegisterAssetInteractor(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public Asset execute(Asset asset) {
        assetRepository.findByCode(asset.code()).ifPresent(existingAsset -> {
            throw new AssetAlreadyRegisteredException(existingAsset.code());
        });
        assetRepository.save(asset);
        return asset;
    }
}
