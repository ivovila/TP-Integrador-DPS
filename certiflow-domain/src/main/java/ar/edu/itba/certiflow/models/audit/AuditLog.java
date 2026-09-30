package ar.edu.itba.certiflow.models.audit;

import java.util.List;

public interface AuditLog<S> {

    void record(S subject, AuditEntry entry);

    List<AuditEntry> historyOf(S subject);

    default void recordAll(S subject, List<AuditEntry> entries) {
        entries.forEach(entry -> record(subject, entry));
    }
}
