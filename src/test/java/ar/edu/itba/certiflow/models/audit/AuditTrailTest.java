package ar.edu.itba.certiflow.models.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.models.inspection.InspectionAudit;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.shared.PersonId;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditTrailTest {

    private final Person inspector = new Person(new PersonId("EMP-002"), "Ines Inspector");

    @Test
    void pullingHandsOverThePendingEntriesInOrderAndLeavesTheTrailEmpty() {
        AuditTrail trail = new AuditTrail();
        trail.record(InspectionAudit.ANSWER_RECORDED, inspector, Instant.EPOCH, "EXT-01 = 7.00");
        trail.record(InspectionAudit.CLOSED, inspector, Instant.EPOCH.plusSeconds(60), "");

        List<AuditEntry> pulled = trail.pull();

        assertEquals(List.of(InspectionAudit.ANSWER_RECORDED, InspectionAudit.CLOSED), pulled.stream().map(AuditEntry::action).toList());
        assertTrue(trail.pull().isEmpty());
    }
}
