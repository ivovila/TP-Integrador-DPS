package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetUseCase;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.Clock;

public final class RegisterAssetUseCaseImpl implements RegisterAssetUseCase {

    private final AssetRepository assetRepository;
    private final AssetService assetService;
    private final CertificationPolicyResolver certificationPolicies;
    private final Clock clock;

    public RegisterAssetUseCaseImpl(AssetRepository assetRepository, AssetService assetService,
                                    CertificationPolicyResolver certificationPolicies, Clock clock) {
        this.assetRepository = assetRepository;
        this.assetService = assetService;
        this.certificationPolicies = certificationPolicies;
        this.clock = clock;
    }

    @Override
    public Asset execute(RegisterAssetCommand command) {
        ensureJurisdictionIsConfigured(command.jurisdictionCode());
        Asset asset = new Asset(command.code(), command.name(), command.type(), command.location(), command.responsible(),
                command.characteristics(), command.jurisdictionCode(), command.subsystems());
        Asset registeredAsset = assetService.register(asset, new Stamp(command.registeredBy(), clock.instant()));
        assetRepository.save(registeredAsset);
        return registeredAsset;
    }

    /** Un activo sin política configurada no podría certificarse nunca, así que el alta se rechaza acá. */
    private void ensureJurisdictionIsConfigured(JurisdictionCode jurisdictionCode) {
        if (certificationPolicies.find(jurisdictionCode).isEmpty()) {
            throw new RequiredValueMissingException("No certification policy is configured for jurisdiction "
                    + jurisdictionCode.value());
        }
    }
}
