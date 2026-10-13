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
        return new Criterion<>(new CriterionCode(code), "Check", new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());
    }
    private Criterion<YesNoAnswer> criterion(String code, SubsystemCode subsystem) {
        return new Criterion<>(new CriterionCode(code), "Check", new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of(), new InSubsystem(subsystem));
    }
    private SchemaVersion versionWith(Section section) {
        return new SchemaVersion(1, new SchemaSections(List.of(section)), Instant.EPOCH, Instant.EPOCH);
    }
    @Test void criterionOverridesSectionAndOthersInherit() {
        var section = new Section(new SectionTitle("Checks"), List.of(criterion("A"), criterion("B", p)), new InSubsystem(e));
        var version = versionWith(section);
        version.ensureRequires(Set.of(e, p));
        assertEquals(Set.of(e, p), version.requiredSubsystems());
        assertEquals(new InSubsystem(e), version.associationOf(version.criterion(new CriterionCode("A"))));
        assertEquals(new InSubsystem(p), version.associationOf(version.criterion(new CriterionCode("B"))));
        assertEquals(List.of(new CriterionCode("A")), version.criteriaFor(e).stream().map(Criterion::code).toList());
    }
    @Test void unassignedCriteriaAndEmptyRequiredSubsystemsAreRejected() {
        var section = new Section(new SectionTitle("Checks"), List.of(criterion("A")));
        assertThrows(InvalidSchemaException.class, () -> versionWith(section).ensureRequires(Set.of(e)));
        var assigned = new Section(new SectionTitle("Checks"), List.of(criterion("A", e)));
        assertThrows(InvalidSchemaException.class, () -> versionWith(assigned).ensureRequires(Set.of(e, p)));
        versionWith(section).ensureRequires(Set.of());
    }
}
