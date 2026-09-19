package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditService;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedInspectionGenerator {

    private final AuditTrail auditTrail;

    public AuditedInspectionGenerator(AuditService auditService) {
        this.auditTrail = new AuditTrail(auditService);
    }

    public Inspection generate(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                               Person assignedBy, Instant assignedAt) {
        Inspection inspection = new AuditedInspection(new StandardInspection(asset, assignment, schemaVersion), auditTrail);
        auditTrail.record(inspection, InspectionAudit.ASSIGNED, assignedBy, assignedAt,
                "Schema version " + schemaVersion.number());
        return inspection;
    }
}
