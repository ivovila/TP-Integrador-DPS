package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryAuditService implements AuditService {

    private final List<AuditEntry> entries = new ArrayList<>();

    @Override
    public void record(AuditEntry entry) {
        entries.add(entry);
    }

    @Override
    public List<AuditEntry> entriesFor(Object subject) {
        return entries.stream().filter(entry -> entry.concerns(subject)).toList();
    }
}
