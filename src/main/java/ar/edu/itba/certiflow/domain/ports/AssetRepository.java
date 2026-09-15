package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;

public interface AssetRepository {

    Optional<Asset> findById(AssetId id);

    default Asset getById(AssetId id) {
        return findById(id).orElseThrow(() -> new NotFoundException("el activo", id.value()));
    }

    void save(Asset asset);
}
