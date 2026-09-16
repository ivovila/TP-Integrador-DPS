package ar.edu.itba.certiflow.infrastructure.memory;

import ar.edu.itba.certiflow.application.port.AssetRepository;
import ar.edu.itba.certiflow.domain.Asset;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Single-process adapter; values are immutable snapshots. */
public final class InMemoryAssetRepository implements AssetRepository {
    private final Map<UUID, Asset> values = new LinkedHashMap<>();

    public Optional<Asset> findById(UUID id) {
        return Optional.ofNullable(values.get(id));
    }

    public void save(Asset value) {
        values.put(value.id(), value);
    }

    public List<Asset> findAll() {
        return List.copyOf(values.values());
    }
}
