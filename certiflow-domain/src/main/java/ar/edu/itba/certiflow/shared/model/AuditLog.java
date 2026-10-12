package ar.edu.itba.certiflow.shared.model;

import java.util.List;

public interface AuditLog<S> {

    void record(S subject, AuditEntry entry);

    List<AuditEntry> historyOf(S subject);

    default void recordAll(S subject, List<AuditEntry> entries) {
        entries.forEach(entry -> record(subject, entry));
    }
}
