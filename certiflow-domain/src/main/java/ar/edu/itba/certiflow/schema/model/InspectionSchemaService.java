package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

public final class InspectionSchemaService {

    private final AuditLog<AssetType> auditLog;

    public InspectionSchemaService(AuditLog<AssetType> auditLog) {
        this.auditLog = auditLog;
    }

    public InspectionSchema create(String name, AssetType assetType, List<Section> sections, Person createdBy, Instant createdAt) {
        InspectionSchema schema = new InspectionSchema(name, assetType, sections, createdAt);
        auditLog.record(assetType, new AuditEntry(SchemaAudit.CREATED, createdBy, createdAt, name + ", version 1"));
        return schema;
    }

    public InspectionSchema addVersion(InspectionSchema schema, List<Section> sections, Person addedBy, Instant addedAt) {
        InspectionSchema updatedSchema = schema.withNewVersion(sections, addedAt);
        auditLog.record(schema.assetType(), new AuditEntry(SchemaAudit.VERSION_ADDED, addedBy, addedAt,
                "Version " + updatedSchema.currentVersion().number()));
        return updatedSchema;
    }
}
