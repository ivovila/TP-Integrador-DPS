package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class InMemoryAssetRepository implements AssetRepository {

    private final Map<AssetCode, Asset> assets = new LinkedHashMap<>();

    @Override
    public void save(Asset asset) {
        assets.put(asset.code(), asset);
    }

    @Override
    public Optional<Asset> findByCode(AssetCode code) {
        return Optional.ofNullable(assets.get(code));
    }

    @Override
    public List<Asset> findAll() {
        return List.copyOf(assets.values());
    }
}
