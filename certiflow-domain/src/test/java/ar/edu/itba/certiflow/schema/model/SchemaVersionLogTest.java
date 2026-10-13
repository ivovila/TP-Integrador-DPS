package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchemaVersionLogTest {

    private final List<Section> sections = List.of(new Section("General", List.of(new Criterion<>("GEN-01", "Seal intact",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of()))));

    @Test
    void appendingNumbersTheNextVersionAndKeepsTheOriginalLog() {
        SchemaVersionLog original = new SchemaVersionLog(
                List.of(new SchemaVersion(1, new SchemaSections(sections), Instant.EPOCH, Instant.EPOCH)));

        SchemaVersionLog appended = original.append(sections, Instant.EPOCH.plusSeconds(60), Instant.EPOCH.plusSeconds(60), Set.of());

        assertEquals(1, original.versions().size());
        assertEquals(2, appended.latestPublished().number());
    }

    @Test
    void logMustStartAtOneAndHaveNoGaps() {
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersionLog(List.of()));
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersionLog(
                List.of(new SchemaVersion(2, new SchemaSections(sections), Instant.EPOCH, Instant.EPOCH))));
    }
}
