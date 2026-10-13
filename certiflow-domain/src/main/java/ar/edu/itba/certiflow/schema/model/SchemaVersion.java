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

public record SchemaVersion(int number, List<Section> sections, Instant publishedAt, Instant effectiveFrom, Set<SubsystemCode> requiredSubsystems) {

    public SchemaVersion(int number, List<Section> sections, Instant publishedAt) {
        this(number, sections, publishedAt, publishedAt, Set.of());
    }

    public SchemaVersion(int number, List<Section> sections, Instant publishedAt, Instant effectiveFrom) {
        this(number, sections, publishedAt, effectiveFrom, Set.of());
    }

    public SchemaVersion {
        requiredSubsystems = Set.copyOf(requiredSubsystems);
        Set<SubsystemCode> assigned = new HashSet<>();
        for (Section section : sections) {
            for (Criterion<?> criterion : section.criteria()) {
                var code = criterion.subsystemCode().or(section::subsystemCode);
                if (!requiredSubsystems.isEmpty() && code.isEmpty()) {
                    throw new InvalidSchemaException("Every criterion must have a subsystem");
                }
                code.ifPresent(assigned::add);
            }
        }
        if (!assigned.equals(requiredSubsystems)) {
            throw new InvalidSchemaException("Required subsystems must match the criterion associations");
        }
        if (sections.stream().map(Section::title).distinct().count() != sections.size()) {
            throw new InvalidSchemaException("Section titles must be unique");
        }
        publishedAt = StoredInstant.normalize(publishedAt);
        effectiveFrom = StoredInstant.normalize(effectiveFrom);
        if (effectiveFrom.isBefore(publishedAt)) {
            throw new InvalidSchemaException("Effective date cannot precede publication");
        }
        sections = List.copyOf(sections);
        if (sections.isEmpty()) {
            throw new InvalidSchemaException("A schema version needs at least one section");
        }
        List<String> criterionCodes = sections.stream()
                .flatMap(section -> section.criteria().stream())
                .map(criterion -> criterion.code())
                .toList();
        if (new HashSet<>(criterionCodes).size() != criterionCodes.size()) {
            throw new InvalidSchemaException("Criterion codes must be unique within a schema version");
        }
    }

    public Optional<SubsystemCode> subsystemOf(String criterionCode) {
        Criterion<?> criterion = criterion(criterionCode);
        return sections.stream().filter(section -> section.criteria().contains(criterion)).findFirst()
                .flatMap(section -> criterion.subsystemCode().or(section::subsystemCode));
    }

    public List<Criterion<?>> criteriaFor(SubsystemCode code) {
        return criteria().stream().filter(criterion -> subsystemOf(criterion.code()).filter(code::equals).isPresent()).toList();
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
