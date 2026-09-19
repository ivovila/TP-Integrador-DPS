package ar.edu.itba.certiflow.models.audit;

import java.util.List;

public interface AuditService {

    void record(AuditEntry entry);

    List<AuditEntry> entriesFor(Object subject);
}
