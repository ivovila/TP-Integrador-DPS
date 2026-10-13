package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ar.edu.itba.certiflow.infrastructure.support.DomainSamples;
import ar.edu.itba.certiflow.inspection.model.InspectionAudit;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditActionCatalog;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class JpaAuditLogTest {

    @Autowired
    private SpringDataAuditEntryRepository entries;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AuditActionCatalog auditActions = new AuditActionCatalog(List.of(InspectionAudit.values()));

    @Test
    void historyIsReadBackInOrderAndOnlyForItsSubject() {
        JpaAuditLog<InspectionId> log = new JpaAuditLog<>(entries, "inspection", InspectionId::value, auditActions);
        AuditEntry assigned = new AuditEntry(InspectionAudit.ASSIGNED, new Stamp(DomainSamples.PLANNER, DomainSamples.T0), "Schema version 1");
        AuditEntry closed = new AuditEntry(InspectionAudit.CLOSED, new Stamp(DomainSamples.INSPECTOR, DomainSamples.T0.plusSeconds(60)), "");

        log.recordAll(new InspectionId("I-1"), List.of(assigned, closed));
        log.record(new InspectionId("I-2"), assigned);

        assertEquals(List.of(assigned, closed), log.historyOf(new InspectionId("I-1")));
        assertEquals(List.of("InspectionAudit.ASSIGNED", "InspectionAudit.CLOSED", "InspectionAudit.ASSIGNED"),
                storedActionCodes());
        assertEquals(List.of(), new JpaAuditLog<InspectionId>(entries, "finding", InspectionId::value, auditActions)
                .historyOf(new InspectionId("I-1")));
    }

    private List<String> storedActionCodes() {
        entries.flush();
        return jdbcTemplate.queryForList("select action from audit_entry order by id", String.class);
    }
}
