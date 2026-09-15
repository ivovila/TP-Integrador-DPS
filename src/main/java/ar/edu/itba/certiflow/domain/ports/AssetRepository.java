package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;

public interface AssetRepository {

    Optional<Asset> findById(AssetId id);

    void save(Asset asset);
}
