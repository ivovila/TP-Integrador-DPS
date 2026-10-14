package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
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
    public Asset execute(RegisterAssetCommand command) {
        Asset asset = new Asset(command.code(), command.name(), command.type(), command.location(), command.responsible(),
                command.characteristics(), command.jurisdictionCode(), command.subsystems());
        Asset registeredAsset = assetService.register(asset, new Stamp(command.registeredBy(), clock.instant()));
        assetRepository.add(registeredAsset);
        return registeredAsset;
    }
}
