package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryAuditLog<S> implements AuditLog<S> {

    private final Map<S, List<AuditEntry>> histories = new HashMap<>();

    @Override
    public void record(S subject, AuditEntry entry) {
        histories.computeIfAbsent(subject, newSubject -> new ArrayList<>()).add(entry);
    }

    @Override
    public List<AuditEntry> historyOf(S subject) {
        return List.copyOf(histories.getOrDefault(subject, List.of()));
    }
}
