package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import java.util.List;

public final class ListAssetsUseCaseImpl implements ListAssetsUseCase {

    private final AssetRepository assetRepository;

    public ListAssetsUseCaseImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public List<Asset> execute() {
        return assetRepository.findAll();
    }

    @Override
    public List<Asset> execute(AssetType type) {
        return assetRepository.findAll().stream().filter(asset -> asset.type().equals(type)).toList();
    }
}
