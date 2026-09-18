package ar.edu.itba.certiflow.application.asset;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.asset.AssetCode;
import java.util.Optional;

public interface AssetRepository {

    void save(Asset asset);

    Optional<Asset> findByCode(AssetCode code);
}
