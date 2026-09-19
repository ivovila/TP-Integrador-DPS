package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryAuditLog<S> implements AuditLog<S> {

    private final Map<S, List<AuditEntry>> histories = new IdentityHashMap<>();

    @Override
    public void record(S subject, AuditEntry entry) {
        histories.computeIfAbsent(subject, newSubject -> new ArrayList<>()).add(entry);
    }

    @Override
    public List<AuditEntry> historyOf(S subject) {
        return List.copyOf(histories.getOrDefault(subject, List.of()));
    }
}
