package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class InspectionSchemaService {

    private final AuditLog<AssetType> auditLog;

    public InspectionSchemaService(AuditLog<AssetType> auditLog) {
        this.auditLog = auditLog;
    }

    public InspectionSchema create(String name, AssetType assetType, List<Section> sections, Person createdBy, Instant createdAt) {
        return create(name, assetType, sections, createdBy, createdAt, Set.of());
    }

    public InspectionSchema create(String name, AssetType assetType, List<Section> sections, Person createdBy,
            Instant createdAt, Set<SubsystemCode> requiredSubsystems) {
        InspectionSchema schema = new InspectionSchema(name, assetType,
                new SchemaVersionLog(List.of(new SchemaVersion(1, sections, createdAt, createdAt, requiredSubsystems))));
        auditLog.record(assetType, new AuditEntry(SchemaAudit.CREATED, createdBy, createdAt, name + ", version 1"));
        return schema;
    }

    public InspectionSchema addVersion(InspectionSchema schema, List<Section> sections, Person addedBy, Instant addedAt) {
        return addVersion(schema, sections, addedBy, addedAt, addedAt);
    }

    public InspectionSchema addVersion(InspectionSchema schema, List<Section> sections, Person addedBy,
                                       Instant addedAt, Instant effectiveFrom) {
        return addVersion(schema, sections, addedBy, addedAt, effectiveFrom, Set.of());
    }

    public InspectionSchema addVersion(InspectionSchema schema, List<Section> sections, Person addedBy, Instant addedAt,
            Instant effectiveFrom, Set<SubsystemCode> requiredSubsystems) {
        InspectionSchema updatedSchema = schema.withNewVersion(sections, addedAt, effectiveFrom, requiredSubsystems);
        auditLog.record(schema.assetType(), new AuditEntry(SchemaAudit.VERSION_ADDED, addedBy, addedAt,
                "Version " + updatedSchema.latestPublishedVersion().number() + ", publishedAt " + addedAt + ", effectiveFrom " + effectiveFrom));
        return updatedSchema;
    }
}
