package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class InspectionSchemaService {

    private final AuditLog<AssetType> auditLog;

    public InspectionSchemaService(AuditLog<AssetType> auditLog) {
        this.auditLog = auditLog;
    }

    public InspectionSchema create(String name, AssetType assetType, List<Section> sections, Stamp created,
            Set<SubsystemCode> requiredSubsystems) {
        SchemaVersion firstVersion = new SchemaVersion(1, new SchemaSections(sections), created.at(), created.at());
        firstVersion.ensureRequires(requiredSubsystems);
        InspectionSchema schema = new InspectionSchema(name, assetType, new SchemaVersionLog(List.of(firstVersion)));
        auditLog.record(assetType, new AuditEntry(SchemaAudit.CREATED, created, name + ", version 1"));
        return schema;
    }

    public InspectionSchema addVersion(InspectionSchema schema, List<Section> sections, Stamp added,
            Instant effectiveFrom, Set<SubsystemCode> requiredSubsystems) {
        InspectionSchema updatedSchema = schema.withNewVersion(sections, added.at(), effectiveFrom, requiredSubsystems);
        auditLog.record(schema.assetType(), new AuditEntry(SchemaAudit.VERSION_ADDED, added,
                "Version " + updatedSchema.latestPublishedVersion().number() + ", publishedAt " + added.at() + ", effectiveFrom " + effectiveFrom));
        return updatedSchema;
    }
}
