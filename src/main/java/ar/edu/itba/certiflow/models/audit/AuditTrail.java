package ar.edu.itba.certiflow.models.audit;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class AuditTrail {

    private final List<AuditEntry> pendingEntries = new ArrayList<>();

    public void record(AuditAction action, Person performedBy, Instant occurredAt, String detail) {
        pendingEntries.add(new AuditEntry(action, performedBy, occurredAt, detail));
    }

    public List<AuditEntry> pull() {
        List<AuditEntry> pulledEntries = List.copyOf(pendingEntries);
        pendingEntries.clear();
        return pulledEntries;
    }
}
