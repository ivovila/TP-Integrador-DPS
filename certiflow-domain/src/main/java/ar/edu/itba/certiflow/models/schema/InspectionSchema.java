package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.List;

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

    public SchemaVersion currentVersion() {
        return versionLog.current();
    }

    public List<SchemaVersion> versions() {
        return versionLog.versions();
    }

    public boolean appliesTo(AssetType type) {
        return assetType.equals(type);
    }
}
