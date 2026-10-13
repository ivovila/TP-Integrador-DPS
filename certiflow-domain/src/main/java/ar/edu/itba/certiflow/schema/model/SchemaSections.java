package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssignedSubsystems;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.CriterionNotInSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.SectionNotInSchemaException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Las secciones de una versión. Los títulos de sección y los códigos de criterio no se repiten dentro de una
 * versión, y por eso la colección puede responder por título y por código.
 */
public record SchemaSections(List<Section> all) {

    public SchemaSections {
        all = List.copyOf(all);
        if (all.isEmpty()) {
            throw new InvalidSchemaException("A schema version needs at least one section");
        }
        if (all.stream().map(Section::title).distinct().count() != all.size()) {
            throw new InvalidSchemaException("Section titles must be unique");
        }
        List<CriterionCode> criterionCodes = all.stream()
                .flatMap(section -> section.criteria().stream())
                .map(Criterion::code)
                .toList();
        if (criterionCodes.stream().distinct().count() != criterionCodes.size()) {
            throw new InvalidSchemaException("Criterion codes must be unique within a schema version");
        }
    }

    public List<Criterion<?>> criteria() {
        return all.stream().flatMap(section -> section.criteria().stream()).toList();
    }

    public Section withTitle(SectionTitle title) {
        return all.stream()
                .filter(section -> section.title().equals(title))
                .findFirst()
                .orElseThrow(() -> new SectionNotInSchemaException(title));
    }

    public Criterion<?> withCode(CriterionCode criterionCode) {
        return criteria().stream()
                .filter(criterion -> criterion.code().equals(criterionCode))
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(criterionCode));
    }

    public void ensureContains(Criterion<?> criterion) {
        if (!criteria().contains(criterion)) {
            throw new CriterionNotInSchemaException(criterion);
        }
    }

    /** El subsistema de un criterio: el propio o, si no tiene, el de su sección. */
    public SubsystemAssociation associationOf(Criterion<?> criterion) {
        return criterion.subsystem().orElse(sectionOf(criterion).subsystem());
    }

    public List<Criterion<?>> assignedTo(SubsystemCode subsystemCode) {
        return criteria().stream()
                .filter(criterion -> associationOf(criterion).isAssignedTo(subsystemCode))
                .toList();
    }

    public Set<SubsystemCode> assignedSubsystems() {
        return criteria().stream()
                .flatMap(criterion -> associationOf(criterion).describedBy(new AssignedSubsystems()).stream())
                .collect(Collectors.toSet());
    }

    public boolean hasCriteriaWithoutSubsystem() {
        return criteria().stream()
                .anyMatch(criterion -> associationOf(criterion).describedBy(new AssignedSubsystems()).isEmpty());
    }

    private Section sectionOf(Criterion<?> criterion) {
        return all.stream()
                .filter(section -> section.criteria().contains(criterion))
                .findFirst()
                .orElseThrow(() -> new CriterionNotInSchemaException(criterion));
    }
}
