package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.exceptions.InvalidSchemaException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InspectionSchema {

    private final String name;
    private final AssetType assetType;
    private final List<SchemaVersion> versions = new ArrayList<>();

    public InspectionSchema(String name, AssetType assetType, List<Section> sections, Instant at) {
        this.name = name;
        this.assetType = assetType;
        if (name.isBlank()) {
            throw new InvalidSchemaException("An inspection schema needs a name");
        }
        publish(sections, at);
    }

    public SchemaVersion publish(List<Section> sections, Instant at) {
        SchemaVersion version = new SchemaVersion(versions.size() + 1, sections, at);
        versions.add(version);
        return version;
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
