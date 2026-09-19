package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditService;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditService implements AuditService {

    private final List<AuditEntry> recordedEntries = new ArrayList<>();

    @Override
    public void record(AuditEntry entry) {
        recordedEntries.add(entry);
    }

    @Override
    public List<AuditEntry> entriesFor(Object subject) {
        return recordedEntries.stream().filter(entry -> entry.concerns(subject)).toList();
    }
}
