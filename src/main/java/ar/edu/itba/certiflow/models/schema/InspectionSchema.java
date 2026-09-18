package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.evaluation.Criterion;
import java.util.List;

public record InspectionSchema(AssetType assetType, List<Section> sections) {
    public boolean includes(Criterion<?> criterion) {
        return sections.stream()
                .flatMap(section -> section.criteria().stream())
                .anyMatch(schemaCriterion -> schemaCriterion.id().equals(criterion.id()));
    }
}
