package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.FindAssetUseCase;

public final class FindAssetUseCaseImpl implements FindAssetUseCase {

    private final AssetRepository assetRepository;

    public FindAssetUseCaseImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public Asset execute(AssetCode code) {
        return assetRepository.findByCode(code).orElseThrow(() -> new AssetNotFoundException(code));
    }
}
