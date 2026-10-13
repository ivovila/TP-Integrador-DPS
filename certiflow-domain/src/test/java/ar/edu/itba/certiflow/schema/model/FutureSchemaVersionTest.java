package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.certiflow.schema.model.exceptions.*;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Set;

class FutureSchemaVersionTest {
    private final List<Section> sections = List.of(new Section("General", List.of(new Criterion<>("G", "Seal",
            new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of()))));
    private final Instant start = Instant.parse("2026-10-12T12:00:00Z");

    private SchemaVersionLog logStartingAt(Instant publishedAt) {
        return new SchemaVersionLog(List.of(new SchemaVersion(1, new SchemaSections(sections), publishedAt, publishedAt)));
    }

    @ParameterizedTest
    @CsvSource({"59,1", "60,2", "61,2", "119,2", "120,3", "121,3"})
    void resolvesHalfOpenIntervals(int seconds, int number) {
        var log = logStartingAt(start)
                .append(sections, start.plusSeconds(1), start.plusSeconds(60), Set.of())
                .append(sections, start.plusSeconds(2), start.plusSeconds(120), Set.of());
        assertEquals(number, log.versionAt(start.plusSeconds(seconds)).number());
        assertThrows(NoEffectiveSchemaVersionException.class, () -> log.versionAt(start.minusNanos(1000)));
    }

    @Test
    void rejectsRetroactiveEqualAndOutOfOrderDates() {
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersion(1, new SchemaSections(sections), start, start.minusSeconds(1)));
        var log = logStartingAt(start).append(sections, start.plusSeconds(1), start.plusSeconds(60), Set.of());
        assertThrows(SchemaEffectiveDateConflictException.class, () -> log.append(sections, start.plusSeconds(2), start.plusSeconds(60), Set.of()));
        assertThrows(SchemaEffectiveDateConflictException.class, () -> log.append(sections, start.plusSeconds(2), start.plusSeconds(30), Set.of()));
    }
}
