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
        InspectionSchema schema = new InspectionSchema(name, assetType,
                new SchemaVersionLog(sections, created.instant(), requiredSubsystems));
        auditLog.record(assetType, new AuditEntry(SchemaAudit.CREATED, created, name + ", version 1"));
        return schema;
    }

    public InspectionSchema publishVersion(InspectionSchema schema, List<Section> sections, Stamp published,
                                           Instant effectiveFrom, Set<SubsystemCode> requiredSubsystems) {
        InspectionSchema updatedSchema = schema.withNewVersion(sections, published.instant(), effectiveFrom,
                requiredSubsystems);
        auditLog.record(schema.assetType(), new AuditEntry(SchemaAudit.VERSION_ADDED, published,
                "Version " + updatedSchema.latestPublishedVersion().number() + ", publishedAt " + published.instant()
                        + ", effectiveFrom " + effectiveFrom));
        return updatedSchema;
    }
}
