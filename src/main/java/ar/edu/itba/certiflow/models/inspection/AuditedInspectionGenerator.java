package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedInspectionGenerator {

    private final AuditLog<Inspection> auditLog;

    public AuditedInspectionGenerator(AuditLog<Inspection> auditLog) {
        this.auditLog = auditLog;
    }

    public Inspection generate(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                               Person assignedBy, Instant assignedAt) {
        Inspection inspection = new AuditedInspection(new StandardInspection(asset, assignment, schemaVersion), auditLog);
        auditLog.record(inspection, new AuditEntry(InspectionAudit.ASSIGNED, assignedBy, assignedAt,
                "Schema version " + schemaVersion.number()));
        return inspection;
    }
}
