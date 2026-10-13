package ar.edu.itba.certiflow.schema.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ar.edu.itba.certiflow.catalog.model.InSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SchemaSectionsTest {

    private static final SubsystemCode ELECTRICAL = new SubsystemCode("ELEC");
    private static final SubsystemCode PRESSURE = new SubsystemCode("PRES");

    private final Criterion<YesNoAnswer> grounding = criterion("GROUNDING");
    private final Criterion<YesNoAnswer> reliefValve = criterion("RELIEF-VALVE");
    private final Criterion<YesNoAnswer> signage = criterion("SIGNAGE");

    private Criterion<YesNoAnswer> criterion(String code) {
        return new Criterion<>(new CriterionCode(code), "Check " + code, new YesNoRule(YesNoAnswer.YES), StandardSeverity.MAJOR, List.of());
    }

    @Test
    void sectionsAndCriteriaAreFoundByTitleAndCode() {
        Section electrical = new Section(new SectionTitle("Electrical"), List.of(grounding));
        SchemaSections sections = new SchemaSections(List.of(electrical, new Section(new SectionTitle("Pressure"), List.of(reliefValve))));

        assertEquals(electrical, sections.withTitle(new SectionTitle("Electrical")));
        assertEquals(reliefValve, sections.withCode(new CriterionCode("RELIEF-VALVE")));
        assertEquals(List.of(grounding, reliefValve), sections.criteria());
        assertThrows(SectionNotInSchemaException.class, () -> sections.withTitle(new SectionTitle("Signage")));
        assertThrows(CriterionNotInSchemaException.class, () -> sections.withCode(new CriterionCode("SIGNAGE")));
        assertThrows(CriterionNotInSchemaException.class, () -> sections.ensureContains(signage));
    }

    @Test
    void theAssignedSubsystemsAreThoseOfTheCriteriaIncludingTheOnesInheritedFromTheirSection() {
        SchemaSections sections = new SchemaSections(List.of(
                new Section(new SectionTitle("Electrical"), List.of(grounding), new InSubsystem(ELECTRICAL)),
                new Section(new SectionTitle("Pressure"), List.of(reliefValve), new InSubsystem(PRESSURE))));

        assertEquals(Set.of(ELECTRICAL, PRESSURE), sections.assignedSubsystems());
        assertEquals(List.of(grounding), sections.assignedTo(ELECTRICAL));
        assertFalse(sections.hasCriteriaWithoutSubsystem());
    }

    @Test
    void aCriterionWithoutSubsystemInASectionWithoutSubsystemIsNoticed() {
        SchemaSections sections = new SchemaSections(List.of(
                new Section(new SectionTitle("Electrical"), List.of(grounding), new InSubsystem(ELECTRICAL)),
                new Section(new SectionTitle("Signage"), List.of(signage))));

        assertTrue(sections.hasCriteriaWithoutSubsystem());
        assertEquals(Set.of(ELECTRICAL), sections.assignedSubsystems());
    }

    @Test
    void aVersionNeedsSectionsWithDifferentTitles() {
        assertThrows(InvalidSchemaException.class, () -> new SchemaSections(List.of()));
        assertThrows(InvalidSchemaException.class, () -> new SchemaSections(List.of(
                new Section(new SectionTitle("Electrical"), List.of(grounding)), new Section(new SectionTitle("Electrical"), List.of(reliefValve)))));
    }
}
