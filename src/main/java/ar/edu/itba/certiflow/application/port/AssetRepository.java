package ar.edu.itba.certiflow.application.port;

import ar.edu.itba.certiflow.domain.Asset;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** save replaces the snapshot identified by id; missing lookups return Optional.empty(). */
public interface AssetRepository {
    Optional<Asset> findById(UUID id);

    void save(Asset value);

    List<Asset> findAll();
}
