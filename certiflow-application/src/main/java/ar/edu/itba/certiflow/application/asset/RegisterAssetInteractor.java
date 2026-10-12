package ar.edu.itba.certiflow.application.asset;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetService;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.usecases.asset.RegisterAsset;
import java.time.Clock;

public final class RegisterAssetInteractor implements RegisterAsset {

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final Clock clock;

    public RegisterAssetInteractor(AssetRepository assetRepository, AssetService assetService, Clock clock) {
        this.assetRepository = assetRepository;
        this.assetService = assetService;
        this.clock = clock;
    }

    @Override
    public Asset execute(Asset asset, Person registeredBy) {
        Asset registeredAsset = assetService.register(asset, assetRepository.findByCode(asset.code()), registeredBy,
                clock.instant());
        assetRepository.save(registeredAsset);
        return registeredAsset;
    }
}
