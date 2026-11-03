package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionAlreadyCancelledException;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaVersionAlreadyInForceException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record SchemaVersion(int number, SchemaSections sections, Instant publishedAt, Instant effectiveFrom,
                            VersionCancellationState cancellation) {

    public SchemaVersion {
        if (effectiveFrom.isBefore(publishedAt)) {
            throw new InvalidSchemaException("Effective date cannot precede publication");
        }
    }

    public SchemaVersion(int number, SchemaSections sections, Instant publishedAt, Instant effectiveFrom) {
        this(number, sections, publishedAt, effectiveFrom, new VersionNotCancelled());
    }

    public boolean isCancelled() {
        return cancellation.isCancelled();
    }

    public boolean isInForceAt(Instant instant) {
        return !isCancelled() && !effectiveFrom.isAfter(instant);
    }

    public boolean isCancellableAt(Instant instant) {
        return !isCancelled() && effectiveFrom.isAfter(instant);
    }

    SchemaVersion cancelled(Stamp cancelled, String reason) {
        if (isCancelled()) {
            throw new SchemaVersionAlreadyCancelledException(number);
        }
        if (!effectiveFrom.isAfter(cancelled.instant())) {
            throw new SchemaVersionAlreadyInForceException(number, effectiveFrom);
        }
        return new SchemaVersion(number, sections, publishedAt, effectiveFrom, new VersionCancellation(cancelled, reason));
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

    public Section section(SectionTitle title) {
        return sections.withTitle(title);
    }

    public Criterion<?> criterion(CriterionCode criterionCode) {
        return sections.withCode(criterionCode);
    }

    public void ensureContains(Criterion<?> criterion) {
        sections.ensureContains(criterion);
    }
}
