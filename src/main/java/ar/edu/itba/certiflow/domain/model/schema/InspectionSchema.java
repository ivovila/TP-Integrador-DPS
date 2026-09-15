package ar.edu.itba.certiflow.domain.model.schema;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import lombok.Getter;

@Getter
public class InspectionSchema {

    private final SchemaId id;
    private final String name;
    private final AssetType assetType;
    private final List<Section> sections = new ArrayList<>();
    private int version = 1;

    public InspectionSchema(SchemaId id, String name, AssetType assetType) {
        this.id = id;
        this.name = name;
        this.assetType = assetType;
    }

    public void addSection(Section section) {
        boolean exists = sections.stream().anyMatch(s -> s.getName().equals(section.getName()));
        if (exists) {
            throw new IllegalArgumentException("Ya existe la seccion '" + section.getName() + "'");
        }
        sections.add(section);
        version++;
    }

    public SchemaVersion snapshot() {
        return new SchemaVersion(id, version, name, sections);
    }

    public List<Section> getSections() {
        return List.copyOf(sections);
    }
}
