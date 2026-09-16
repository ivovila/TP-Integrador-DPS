package ar.edu.itba.certiflow.infrastructure.memory;

import ar.edu.itba.certiflow.application.port.InspectionRepository;
import ar.edu.itba.certiflow.domain.Inspection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Single-process adapter; values are immutable snapshots. */
public final class InMemoryInspectionRepository implements InspectionRepository {
    private final Map<UUID, Inspection> values = new LinkedHashMap<>();

    public Optional<Inspection> findById(UUID id) {
        return Optional.ofNullable(values.get(id));
    }

    public void save(Inspection value) {
        values.put(value.id(), value);
    }
}
