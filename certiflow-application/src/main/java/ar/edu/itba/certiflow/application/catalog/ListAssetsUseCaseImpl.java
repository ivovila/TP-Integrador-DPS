package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.usecase.ListAssetsUseCase;
import java.util.List;
import java.util.Optional;

public final class ListAssetsUseCaseImpl implements ListAssetsUseCase {

    private final AssetRepository assetRepository;

    public ListAssetsUseCaseImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public List<Asset> execute(Optional<AssetType> type) {
        return assetRepository.findAll().stream()
                .filter(asset -> type.map(asset.type()::equals).orElse(true))
                .toList();
    }
}
