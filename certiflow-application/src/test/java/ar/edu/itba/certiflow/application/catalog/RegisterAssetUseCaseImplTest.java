package ar.edu.itba.certiflow.application.catalog;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ar.edu.itba.certiflow.application.support.Samples;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetService;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.RegisterAssetCommand;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.SeverityCertificationPolicy;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegisterAssetUseCaseImplTest {

    private static final Asset ASSET = Samples.ASSET;
    private static final RegisterAssetCommand COMMAND = new RegisterAssetCommand(ASSET.code(), ASSET.name(), ASSET.type(),
            ASSET.location(), ASSET.responsible(), ASSET.characteristics(), Samples.PLANNER, ASSET.jurisdictionCode(), List.of());

    @Mock
    private AssetRepository assetRepository;

    private RegisterAssetUseCaseImpl registerAsset(CertificationPolicyResolver policies) {
        return new RegisterAssetUseCaseImpl(assetRepository, new AssetService(Samples.auditLog(), assetRepository), policies,
                Samples.CLOCK);
    }

    @Test
    void aNewAssetIsSaved() {
        when(assetRepository.findByCode(ASSET.code())).thenReturn(Optional.empty());

        registerAsset(code -> Optional.of(SeverityCertificationPolicy.standard())).execute(COMMAND);

        verify(assetRepository).save(any());
    }

    @Test
    void anAssetUnderAJurisdictionWithoutPolicyIsRejectedAndNotSaved() {
        assertThrows(RequiredValueMissingException.class, () -> registerAsset(code -> Optional.empty()).execute(COMMAND));
        verify(assetRepository, never()).save(any());
    }

    @Test
    void aSecondAssetWithTheSameCodeIsRejectedAndNotSaved() {
        when(assetRepository.findByCode(ASSET.code())).thenReturn(Optional.of(ASSET));

        assertThrows(AssetAlreadyRegisteredException.class,
                () -> registerAsset(code -> Optional.of(SeverityCertificationPolicy.standard())).execute(COMMAND));
        verify(assetRepository, never()).save(any());
    }
}
