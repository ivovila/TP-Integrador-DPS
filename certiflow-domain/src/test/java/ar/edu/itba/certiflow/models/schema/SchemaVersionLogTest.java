package ar.edu.itba.certiflow.models.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ar.edu.itba.certiflow.models.evaluation.StandardSeverity;
import ar.edu.itba.certiflow.models.evaluation.YesNoAnswer;
import ar.edu.itba.certiflow.models.evaluation.YesNoRule;
import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchemaVersionLogTest {

    private final List<Section> sections = List.of(new Section("General", List.of(new Criterion<>("GEN-01", "Seal intact",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of()))));

    @Test
    void appendingNumbersTheNextVersionAndKeepsTheOriginalLog() {
        SchemaVersionLog original = SchemaVersionLog.startingWith(sections, Instant.EPOCH);

        SchemaVersionLog appended = original.append(sections, Instant.EPOCH.plusSeconds(60));

        assertEquals(1, original.versions().size());
        assertEquals(2, appended.current().number());
    }

    @Test
    void logMustStartAtOneAndHaveNoGaps() {
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersionLog(List.of()));
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersionLog(List.of(new SchemaVersion(2, sections, Instant.EPOCH))));
    }
}
