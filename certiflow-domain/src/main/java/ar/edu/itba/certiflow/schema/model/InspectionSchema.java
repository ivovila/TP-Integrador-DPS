package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record InspectionSchema(String name, AssetType assetType, SchemaVersionLog versionLog) {

    public InspectionSchema {
        if (name.isBlank()) {
            throw new InvalidSchemaException("An inspection schema needs a name");
        }
    }

    public InspectionSchema(String name, AssetType assetType, List<Section> sections, Instant publishedAt) {
        this(name, assetType, SchemaVersionLog.startingWith(sections, publishedAt));
    }

    public InspectionSchema withNewVersion(List<Section> sections, Instant publishedAt) {
        return new InspectionSchema(name, assetType, versionLog.append(sections, publishedAt));
    }

    public InspectionSchema withNewVersion(List<Section> sections, Instant publishedAt, Instant effectiveFrom) {
        return new InspectionSchema(name, assetType, versionLog.append(sections, publishedAt, effectiveFrom));
    }

    public InspectionSchema withNewVersion(List<Section> sections, Instant publishedAt, Instant effectiveFrom,
            Set<SubsystemCode> requiredSubsystems) {
        return new InspectionSchema(name, assetType, versionLog.append(sections, publishedAt, effectiveFrom, requiredSubsystems));
    }

    public SchemaVersion versionAt(Instant at) {
        return versionLog.versionAt(at);
    }

    public SchemaVersion latestPublishedVersion() {
        return versionLog.latestPublished();
    }


    public List<SchemaVersion> versions() {
        return versionLog.versions();
    }

    public boolean appliesTo(AssetType type) {
        return assetType.equals(type);
    }
}
