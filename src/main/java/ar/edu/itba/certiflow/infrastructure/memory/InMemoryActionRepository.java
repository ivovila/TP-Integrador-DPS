package ar.edu.itba.certiflow.infrastructure.memory;

import ar.edu.itba.certiflow.application.port.ActionRepository;
import ar.edu.itba.certiflow.domain.CorrectiveAction;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Single-process adapter; values are immutable snapshots. */
public final class InMemoryActionRepository implements ActionRepository {
    private final Map<UUID, CorrectiveAction> values = new LinkedHashMap<>();

    public Optional<CorrectiveAction> findById(UUID id) {
        return Optional.ofNullable(values.get(id));
    }

    public void save(CorrectiveAction value) {
        values.put(value.id(), value);
    }

    public List<CorrectiveAction> findByInspection(UUID id) {
        return values.values().stream().filter(a -> a.finding().inspectionId().equals(id)).toList();
    }
}
