package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.exceptions.JurisdictionPolicyNotFoundException;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.Clock;

public final class RegisterAssetUseCaseImpl implements RegisterAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final Clock clock;
    private final CertificationPolicyResolver policies;

    public RegisterAssetUseCaseImpl(AssetRepository assetRepository, AssetService assetService, Clock clock, CertificationPolicyResolver policies) {
        this.assetRepository = assetRepository;
        this.assetService = assetService;
        this.clock = clock;
        this.policies = policies;
    }

    @Override
    public Asset execute(RegisterAssetCommand command) {
        try {
            policies.resolve(command.jurisdictionCode());
        } catch (JurisdictionPolicyNotFoundException missing) {
            throw new RequiredValueMissingException(missing.getMessage());
        }
        Asset asset = new Asset(command.code(), command.name(), command.type(), command.location(), command.responsible(),
                command.characteristics(), command.jurisdictionCode(), command.subsystems());
        Asset registeredAsset = assetService.register(asset, assetRepository.findByCode(asset.code()), command.registeredBy(),
                clock.instant());
        assetRepository.save(registeredAsset);
        return registeredAsset;
    }
}
