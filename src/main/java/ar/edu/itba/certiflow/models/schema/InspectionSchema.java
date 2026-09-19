package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InspectionSchema {

    private final String name;
    private final AssetType assetType;
    private final List<SchemaVersion> versions = new ArrayList<>();

    public InspectionSchema(String name, AssetType assetType, List<Section> sections, Instant publishedAt) {
        this.name = name;
        this.assetType = assetType;
        if (name.isBlank()) {
            throw new InvalidSchemaException("An inspection schema needs a name");
        }
        publish(sections, publishedAt);
    }

    public SchemaVersion publish(List<Section> sections, Instant publishedAt) {
        SchemaVersion publishedVersion = new SchemaVersion(versions.size() + 1, sections, publishedAt);
        versions.add(publishedVersion);
        return publishedVersion;
    }

    public SchemaVersion currentVersion() {
        return versions.getLast();
    }

    public List<SchemaVersion> versions() {
        return List.copyOf(versions);
    }

    public boolean appliesTo(AssetType type) {
        return assetType.equals(type);
    }

    public String name() {
        return name;
    }

    public AssetType assetType() {
        return assetType;
    }
}
