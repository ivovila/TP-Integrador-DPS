package ar.edu.itba.certiflow.models.audit;

import java.util.List;

public interface AuditLog<S> {

    void record(S subject, AuditEntry entry);

    List<AuditEntry> historyOf(S subject);
}
