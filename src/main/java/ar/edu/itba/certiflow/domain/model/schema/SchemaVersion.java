package ar.edu.itba.certiflow.domain.model.schema;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetType;

public record SchemaVersion(SchemaId schemaId, int number, String name, AssetType assetType, List<Section> sections) {

    public SchemaVersion {
        sections = sections.stream().map(Section::copy).toList();
    }

    @Override
    public List<Section> sections() {
        return sections.stream().map(Section::copy).toList();
    }

    public List<Criterion> criteria() {
        return sections.stream()
                .flatMap(section -> section.getCriteria().stream())
                .toList();
    }

    public Optional<Criterion> findCriterion(CriterionId criterionId) {
        return criteria().stream()
                .filter(criterion -> criterion.id().equals(criterionId))
                .findFirst();
    }
}
