package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import java.util.Optional;

public interface AssetRepository {

    void save(Asset asset);

    Optional<Asset> findByCode(AssetCode code);
}
