package ar.edu.itba.certiflow.infrastructure.memory;

import ar.edu.itba.certiflow.application.port.SchemeRepository;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.SchemeVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InMemorySchemeRepository implements SchemeRepository {
    private final Map<UUID, List<SchemeVersion>> values = new LinkedHashMap<>();

    public List<SchemeVersion> versions(UUID id) {
        return List.copyOf(values.getOrDefault(id, List.of()));
    }

    public void publish(SchemeVersion version) {
        var previous = versions(version.schemeId());
        Checks.require(
                previous.stream().noneMatch(v -> v.number() == version.number()),
                "Published version cannot be overwritten");
        var updated = new ArrayList<>(previous);
        updated.add(version);
        values.put(version.schemeId(), List.copyOf(updated));
    }
}
