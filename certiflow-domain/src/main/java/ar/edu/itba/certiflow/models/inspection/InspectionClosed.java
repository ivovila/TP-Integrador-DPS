package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

public record InspectionClosed(InspectionId inspectionId, AssetCode assetCode, Person assetResponsible,
                               List<CriterionResponse<?>> nonConformities, Person closedBy, Instant closedAt) {

    public InspectionClosed {
        nonConformities = List.copyOf(nonConformities);
    }
}
