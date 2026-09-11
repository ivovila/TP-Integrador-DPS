package ar.edu.itba.certiflow.domain.schema;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.certiflow.domain.asset.AssetType;
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

    public void addSection(String sectionName) {
        if (findSection(sectionName) != -1) {
            throw new IllegalArgumentException("Ya existe la seccion '" + sectionName + "'");
        }
        sections.add(new Section(sectionName, List.of()));
        version++;
    }

    public void addCriterion(String sectionName, Criterion criterion) {
        int index = findSection(sectionName);
        if (index == -1) {
            throw new IllegalArgumentException("No existe la seccion '" + sectionName + "'");
        }
        Section section = sections.get(index);
        List<Criterion> criteria = new ArrayList<>(section.criteria());
        criteria.add(criterion);
        sections.set(index, new Section(sectionName, criteria));
        version++;
    }

    public SchemaVersion snapshot() {
        return new SchemaVersion(id, version, name, sections);
    }

    public List<Section> getSections() {
        return List.copyOf(sections);
    }

    private int findSection(String sectionName) {
        for (int i = 0; i < sections.size(); i++) {
            if (sections.get(i).name().equals(sectionName)) {
                return i;
            }
        }
        return -1;
    }
}
