package ar.edu.itba.certiflow.shared.model;

import java.util.ArrayList;
import java.util.List;

public final class AuditTrail {

    private final List<AuditEntry> pendingEntries = new ArrayList<>();

    public void record(AuditAction action, Stamp performed, String detail) {
        pendingEntries.add(new AuditEntry(action, performed, detail));
    }

    public List<AuditEntry> pull() {
        List<AuditEntry> pulledEntries = List.copyOf(pendingEntries);
        pendingEntries.clear();
        return pulledEntries;
    }
}
