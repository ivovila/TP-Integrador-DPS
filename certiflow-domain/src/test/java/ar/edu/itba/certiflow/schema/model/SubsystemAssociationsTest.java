package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.*;

import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;

class SubsystemAssociationsTest {
    private final SubsystemCode e = new SubsystemCode("E");
    private final SubsystemCode p = new SubsystemCode("P");
    private Criterion<YesNoAnswer> criterion(String code) {
        return new Criterion<>(code, "Check", new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());
    }
    private Criterion<YesNoAnswer> criterion(String code, SubsystemCode subsystem) {
        return new Criterion<>(code, "Check", new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of(), new InSubsystem(subsystem));
    }
    @Test void criterionOverridesSectionAndOthersInherit() {
        var section = new Section("Checks", List.of(criterion("A"), criterion("B", p)), new InSubsystem(e));
        var version = new SchemaVersion(1, List.of(section), Instant.EPOCH, Instant.EPOCH, Set.of(e, p));
        assertEquals(new InSubsystem(e), version.associationOf(version.criterion("A")));
        assertEquals(new InSubsystem(p), version.associationOf(version.criterion("B")));
        assertEquals(List.of("A"), version.criteriaFor(e).stream().map(Criterion::code).toList());
    }
    @Test void unassignedCriteriaAndEmptyRequiredSubsystemsAreRejected() {
        var section = new Section("Checks", List.of(criterion("A")));
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersion(1, List.of(section), Instant.EPOCH, Instant.EPOCH, Set.of(e)));
        var assigned = new Section("Checks", List.of(criterion("A", e)));
        assertThrows(InvalidSchemaException.class, () -> new SchemaVersion(1, List.of(assigned), Instant.EPOCH, Instant.EPOCH, Set.of(e, p)));
    }
}
