package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public record SchemaVersion(int number, List<Section> sections, Instant publishedAt, Instant effectiveFrom, Set<SubsystemCode> requiredSubsystems) {

    public SchemaVersion {
        sections = List.copyOf(sections);
        requiredSubsystems = Set.copyOf(requiredSubsystems);
        publishedAt = StoredInstant.normalize(publishedAt);
        effectiveFrom = StoredInstant.normalize(effectiveFrom);
        requireSections(sections);
        requireUniqueSectionTitles(sections);
        requireUniqueCriterionCodes(sections);
        requireSubsystemAssociations(sections, requiredSubsystems);
        requireEffectiveAfterPublication(publishedAt, effectiveFrom);
    }

    private static void requireSections(List<Section> sections) {
        if (sections.isEmpty()) {
            throw new InvalidSchemaException("A schema version needs at least one section");
        }
    }

    private static void requireUniqueSectionTitles(List<Section> sections) {
        if (sections.stream().map(Section::title).distinct().count() != sections.size()) {
            throw new InvalidSchemaException("Section titles must be unique");
        }
    }

    private static void requireUniqueCriterionCodes(List<Section> sections) {
        List<String> codes = sections.stream().flatMap(section -> section.criteria().stream()).map(Criterion::code).toList();
        if (new HashSet<>(codes).size() != codes.size()) {
            throw new InvalidSchemaException("Criterion codes must be unique within a schema version");
        }
    }

    /** Con subsistemas requeridos, cada criterio pertenece a uno, y los asignados son exactamente los requeridos. */
    private static void requireSubsystemAssociations(List<Section> sections, Set<SubsystemCode> requiredSubsystems) {
        List<Optional<SubsystemCode>> associations = sections.stream()
                .flatMap(section -> section.criteria().stream().map(criterion -> subsystemOf(section, criterion)))
                .toList();
        if (!requiredSubsystems.isEmpty() && associations.stream().anyMatch(Optional::isEmpty)) {
            throw new InvalidSchemaException("Every criterion must have a subsystem");
        }
        Set<SubsystemCode> assigned = associations.stream().flatMap(Optional::stream).collect(Collectors.toSet());
        if (!assigned.equals(requiredSubsystems)) {
            throw new InvalidSchemaException("Required subsystems must match the criterion associations");
        }
    }

    private static void requireEffectiveAfterPublication(Instant publishedAt, Instant effectiveFrom) {
        if (effectiveFrom.isBefore(publishedAt)) {
            throw new InvalidSchemaException("Effective date cannot precede publication");
        }
    }

    /** El subsistema de un criterio: el propio, o si no tiene, el de su sección. */
    private static Optional<SubsystemCode> subsystemOf(Section section, Criterion<?> criterion) {
        return criterion.subsystemCode().or(section::subsystemCode);
    }

    public Optional<SubsystemCode> subsystemOf(String criterionCode) {
        Criterion<?> criterion = criterion(criterionCode);
        return sections.stream().filter(section -> section.criteria().contains(criterion)).findFirst()
                .flatMap(section -> subsystemOf(section, criterion));
    }

    public List<Criterion<?>> criteriaFor(SubsystemCode code) {
        return sections.stream()
                .flatMap(section -> section.criteria().stream()
                        .filter(criterion -> subsystemOf(section, criterion).filter(code::equals).isPresent()))
                .toList();
    }


    public List<Criterion<?>> criteria() {
        return sections.stream().flatMap(section -> section.criteria().stream()).toList();
    }

    public Section section(String title) {
        return sections.stream()
                .filter(section -> section.title().equals(title))
                .findFirst()
                .orElseThrow(() -> new SectionNotInSchemaException(title));
    }

    public Criterion<?> criterion(String code) {
        return criteria().stream()
                .filter(criterion -> criterion.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(code));
    }

    public void ensureContains(Criterion<?> criterion) {
        if (!criteria().contains(criterion)) {
            throw new CriterionNotInSchemaException(criterion);
        }
    }
}
