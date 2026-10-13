package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;
import java.util.function.Function;

public class JpaAuditLog<S> implements AuditLog<S> {

    private final SpringDataAuditEntryRepository entries;
    private final String subjectType;
    private final Function<S, String> idOf;

    public JpaAuditLog(SpringDataAuditEntryRepository entries, String subjectType, Function<S, String> idOf) {
        this.entries = entries;
        this.subjectType = subjectType;
        this.idOf = idOf;
    }

    @Override
    public void record(S subject, AuditEntry entry) {
        entries.save(new AuditEntryEntity(subjectType, idOf.apply(subject), entry));
    }

    @Override
    public List<AuditEntry> historyOf(S subject) {
        return entries.findBySubjectTypeAndSubjectIdOrderByIdAsc(subjectType, idOf.apply(subject)).stream()
                .map(AuditEntryEntity::toDomain)
                .toList();
    }
}
