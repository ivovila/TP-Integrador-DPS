package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record AuditEntry(Instant at, String actor, String operation, String detail) {
    public AuditEntry {
        Objects.requireNonNull(at);
        actor = Checks.text(actor, "actor");
        operation = Checks.text(operation, "operation");
        detail = Checks.text(detail, "detail");
    }

    public static List<AuditEntry> append(List<AuditEntry> history, AuditEntry entry) {
        var updated = new ArrayList<>(history);
        updated.add(entry);
        return List.copyOf(updated);
    }
}
