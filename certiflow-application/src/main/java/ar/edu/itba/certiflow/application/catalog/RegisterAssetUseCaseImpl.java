package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import java.time.Clock;

public final class RegisterAssetUseCaseImpl implements RegisterAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final Clock clock;

    public RegisterAssetUseCaseImpl(AssetRepository assetRepository, AssetService assetService, Clock clock) {
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
