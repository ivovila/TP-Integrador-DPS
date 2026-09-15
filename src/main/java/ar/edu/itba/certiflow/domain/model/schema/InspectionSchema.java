package ar.edu.itba.certiflow.domain.model.schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import lombok.Getter;

@Getter
public class InspectionSchema extends AggregateRoot {

    private final SchemaId id;
    private final String name;
    private final AssetType assetType;
    private final List<Section> sections = new ArrayList<>();
    private final List<SchemaVersion> versions = new ArrayList<>();

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
    }

    public SchemaVersion publish(LocalDateTime now) {
        boolean hasCriteria = sections.stream().anyMatch(section -> !section.getCriteria().isEmpty());
        if (!hasCriteria) {
            throw new DomainException("No se puede publicar un esquema sin criterios");
        }
        SchemaVersion version = new SchemaVersion(id, versions.size() + 1, name, assetType, sections);
        versions.add(version);
        recordEvent(new SchemaVersionPublished(id, version.number(), now));
        return version;
    }

    public Optional<SchemaVersion> latestVersion() {
        return versions.isEmpty() ? Optional.empty() : Optional.of(versions.getLast());
    }

    public List<Section> getSections() {
        return List.copyOf(sections);
    }

    public List<SchemaVersion> getVersions() {
        return List.copyOf(versions);
    }
}
