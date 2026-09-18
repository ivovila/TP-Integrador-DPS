package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.audit.AuditTrail;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public final class AuditedInspectionGenerator {

    private final AuditTrail audit;

    public AuditedInspectionGenerator(AuditService auditService) {
        this.audit = new AuditTrail(auditService);
    }

    public Inspection generate(Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                               Person by, Instant at) {
        Inspection inspection = new AuditedInspection(new StandardInspection(asset, assignment, schemaVersion), audit);
        audit.record(inspection, InspectionAudit.ASSIGNED, by, at,
                "Schema version " + schemaVersion.number());
        return inspection;
    }
}
