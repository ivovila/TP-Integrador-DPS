package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaEffectiveDateConflictException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionAlreadyCancelledException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionAlreadyInForceException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionNotFoundException;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CancelledSchemaVersionTest {

    private final AssetType extinguisher = new AssetType("Fire extinguisher");
    private final Person administrator = new Person(new PersonId("EMP-006"), "Ada Admin");
    private final List<Section> sections = List.of(new Section(new SectionTitle("General"), List.of(new Criterion<>(
            new CriterionCode("G"), "Seal", new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of()))));
    private final Instant start = Instant.parse("2026-10-12T12:00:00Z");
    private final Instant nextMonth = start.plusSeconds(30L * 86_400);
    private final RecordingAuditLog auditLog = new RecordingAuditLog();
    private final InspectionSchemaService service = new InspectionSchemaService(auditLog);

    private InspectionSchema schemaWithAPendingVersion() {
        InspectionSchema schema = new InspectionSchema("Extinguisher", extinguisher,
                new SchemaVersionLog(sections, start, Set.of()));
        return schema.withNewVersion(sections, start.plusSeconds(60), nextMonth, Set.of());
    }

    private Stamp at(Instant instant) {
        return new Stamp(administrator, instant);
    }

    @Test
    void aPendingVersionBlocksAnUrgentVersionWithAnEarlierDate() {
        InspectionSchema schema = schemaWithAPendingVersion();

        assertThrows(SchemaEffectiveDateConflictException.class,
                () -> schema.withNewVersion(sections, start.plusSeconds(120), start.plusSeconds(120), Set.of()));
    }

    @Test
    void cancellingThePendingVersionLetsAnUrgentVersionRuleRightAway() {
        Instant urgentAt = start.plusSeconds(120);
        InspectionSchema cancelled = service.cancelVersion(schemaWithAPendingVersion(), 2, at(urgentAt),
                "Criterion G was loaded with the wrong severity");

        InspectionSchema corrected = cancelled.withNewVersion(sections, urgentAt, urgentAt, Set.of());

        assertEquals(3, corrected.latestPublishedVersion().number());
        assertEquals(3, corrected.versionAt(urgentAt).number());
        assertEquals(3, corrected.versionAt(nextMonth).number());
    }

    @Test
    void aCancelledVersionNeverComesIntoForceAndStaysInTheHistory() {
        InspectionSchema cancelled = service.cancelVersion(schemaWithAPendingVersion(), 2, at(start.plusSeconds(120)),
                "Superseded");

        assertEquals(1, cancelled.versionAt(nextMonth).number());
        assertEquals(1, cancelled.latestPublishedVersion().number());
        assertEquals(2, cancelled.versions().size());
        SchemaVersion second = cancelled.version(2);
        assertTrue(second.isCancelled());
        assertEquals(new VersionCancellation(at(start.plusSeconds(120)), "Superseded"), second.cancellation());
        assertFalse(second.isCancellableAt(start.plusSeconds(120)));
    }

    @Test
    void aVersionCanBeRepublishedOnTheDateOfTheCancelledOne() {
        InspectionSchema cancelled = service.cancelVersion(schemaWithAPendingVersion(), 2, at(start.plusSeconds(120)),
                "Superseded");

        InspectionSchema rescheduled = cancelled.withNewVersion(sections, start.plusSeconds(180), nextMonth, Set.of());

        assertEquals(3, rescheduled.versionAt(nextMonth).number());
    }

    @Test
    void cancellingIsAudited() {
        service.cancelVersion(schemaWithAPendingVersion(), 2, at(start.plusSeconds(120)), "Superseded");

        AuditEntry entry = auditLog.entries.getLast();
        assertEquals(SchemaAudit.VERSION_CANCELLED, entry.action());
        assertEquals(at(start.plusSeconds(120)), entry.performed());
        assertEquals("Version 2, effectiveFrom " + nextMonth + ", reason Superseded", entry.detail());
    }

    @Test
    void aVersionAlreadyInForceCannotBeCancelled() {
        InspectionSchema schema = schemaWithAPendingVersion();

        assertThrows(SchemaVersionAlreadyInForceException.class,
                () -> service.cancelVersion(schema, 1, at(start.plusSeconds(120)), "Too late"));
        assertThrows(SchemaVersionAlreadyInForceException.class,
                () -> service.cancelVersion(schema, 2, at(nextMonth), "Too late"));
        assertTrue(auditLog.entries.isEmpty());
    }

    @Test
    void aVersionIsCancelledOnlyOnce() {
        InspectionSchema cancelled = service.cancelVersion(schemaWithAPendingVersion(), 2, at(start.plusSeconds(120)),
                "Superseded");

        assertThrows(SchemaVersionAlreadyCancelledException.class,
                () -> service.cancelVersion(cancelled, 2, at(start.plusSeconds(180)), "Again"));
    }

    @Test
    void aCancellationNeedsAReasonAndAnExistingVersion() {
        InspectionSchema schema = schemaWithAPendingVersion();

        assertThrows(RequiredValueMissingException.class,
                () -> service.cancelVersion(schema, 2, at(start.plusSeconds(120)), " "));
        assertThrows(SchemaVersionNotFoundException.class,
                () -> service.cancelVersion(schema, 3, at(start.plusSeconds(120)), "Missing"));
    }

    private static final class RecordingAuditLog implements AuditLog<AssetType> {

        private final List<AuditEntry> entries = new ArrayList<>();

        @Override
        public void record(AssetType subject, AuditEntry entry) {
            entries.add(entry);
        }

        @Override
        public List<AuditEntry> historyOf(AssetType subject) {
            return List.copyOf(entries);
        }
    }
}
