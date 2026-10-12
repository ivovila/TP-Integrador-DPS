package ar.edu.itba.certiflow.catalog.repository;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import java.util.Optional;

public interface AssetRepository {

    void save(Asset asset);

    Optional<Asset> findByCode(AssetCode code);
}
