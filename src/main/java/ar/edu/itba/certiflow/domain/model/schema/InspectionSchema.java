package ar.edu.itba.certiflow.domain.model.schema;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.EventSource;
import ar.edu.itba.certiflow.domain.model.shared.PendingEvents;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class InspectionSchema implements EventSource {

    private final SchemaId id;
    private final String name;
    private final AssetType assetType;
    private final List<Section> sections = new ArrayList<>();
    private final List<SchemaVersion> versions = new ArrayList<>();
    @Getter(AccessLevel.NONE)
    private final PendingEvents events = new PendingEvents();

    public InspectionSchema(SchemaId id, String name, AssetType assetType) {
        this.id = id;
        this.name = name;
        this.assetType = assetType;
    }

    public void addSection(String sectionName) {
        if (indexOf(sectionName).isPresent()) {
            throw new DomainException("Ya existe la seccion '" + sectionName + "'");
        }
        sections.add(Section.named(sectionName));
    }

    public void addCriterion(String sectionName, Criterion criterion) {
        int index = indexOf(sectionName)
                .orElseThrow(() -> new DomainException("No existe la seccion '" + sectionName + "'"));
        boolean repeated = sections.stream()
                .flatMap(section -> section.criteria().stream())
                .anyMatch(existing -> existing.id().equals(criterion.id()));
        if (repeated) {
            throw new DomainException("El criterio ya forma parte del esquema");
        }
        sections.set(index, sections.get(index).with(criterion));
    }

    public SchemaVersion publish(LocalDateTime now) {
        boolean hasCriteria = sections.stream().anyMatch(section -> !section.criteria().isEmpty());
        if (!hasCriteria) {
            throw new DomainException("No se puede publicar un esquema sin criterios");
        }
        SchemaVersion version = new SchemaVersion(id, versions.size() + 1, name, assetType, sections);
        versions.add(version);
        events.add(new SchemaVersionPublished(id, version.number(), now));
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

    private Optional<Integer> indexOf(String sectionName) {
        return IntStream.range(0, sections.size())
                .filter(i -> sections.get(i).name().equals(sectionName))
                .boxed()
                .findFirst();
    }

    @Override
    public List<DomainEvent> pullEvents() {
        return events.pull();
    }
}
