package ar.edu.itba.certiflow.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.ports.AssetRepository;

public class InMemoryAssetRepository implements AssetRepository {

    private final Map<AssetId, Asset> assets = new HashMap<>();

    @Override
    public Optional<Asset> findById(AssetId id) {
        return Optional.ofNullable(assets.get(id));
    }

    @Override
    public void save(Asset asset) {
        assets.put(asset.getId(), asset);
    }
}
