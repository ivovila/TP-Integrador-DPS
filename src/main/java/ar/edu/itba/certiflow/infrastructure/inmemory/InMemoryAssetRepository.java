package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.application.asset.AssetRepository;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.asset.AssetCode;
import java.util.LinkedHashMap;
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
}
