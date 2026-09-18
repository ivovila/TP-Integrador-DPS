package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;

/**
 * Único punto público donde nace una inspección: compone la implementación de negocio con su
 * decorador de auditoría y registra el alta. Al exigir un AuditService por constructor,
 * no existe forma de obtener una inspección sin auditar.
 */
public final class AuditedInspectionGenerator {

    private final AuditService auditService;

    public AuditedInspectionGenerator(AuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "auditService");
    }

    public Inspection generate(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                               Person by, Instant at) {
        Inspection inspection = new AuditedInspection(new StandardInspection(asset, assignment, schemaVersion),
                auditService);
        auditService.record(new AuditEntry(inspection, InspectionAudit.ASSIGNED, by, at,
                "Schema version " + schemaVersion.number()));
        return inspection;
    }
}
