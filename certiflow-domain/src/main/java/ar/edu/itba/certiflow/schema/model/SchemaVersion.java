package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SchemaVersion(int number, SchemaSections sections, Instant publishedAt, Instant effectiveFrom) {

    public SchemaVersion {
        if (effectiveFrom.isBefore(publishedAt)) {
            throw new InvalidSchemaException("Effective date cannot precede publication");
        }
    }

    /** Los subsistemas que la versión exige son los que sus criterios tienen asignados. */
    public Set<SubsystemCode> requiredSubsystems() {
        return sections.assignedSubsystems();
    }

    /**
     * Quien publica declara qué subsistemas exige la versión, y eso tiene que coincidir con lo que dicen sus
     * criterios: con subsistemas declarados, cada criterio pertenece a uno, y los asignados son exactamente esos.
     */
    public void ensureRequires(Set<SubsystemCode> declaredSubsystems) {
        if (!declaredSubsystems.isEmpty() && sections.hasCriteriaWithoutSubsystem()) {
            throw new InvalidSchemaException("Every criterion must have a subsystem");
        }
        if (!requiredSubsystems().equals(declaredSubsystems)) {
            throw new InvalidSchemaException("Required subsystems must match the criterion associations");
        }
    }

    public SubsystemAssociation associationOf(Criterion<?> criterion) {
        return sections.associationOf(criterion);
    }

    public List<Criterion<?>> criteriaFor(SubsystemCode subsystemCode) {
        return sections.assignedTo(subsystemCode);
    }

    public List<Criterion<?>> criteria() {
        return sections.criteria();
    }

    public Section section(String title) {
        return sections.withTitle(title);
    }

    public Criterion<?> criterion(String criterionCode) {
        return sections.withCode(criterionCode);
    }

    public void ensureContains(Criterion<?> criterion) {
        sections.ensureContains(criterion);
    }
}
