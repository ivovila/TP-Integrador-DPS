package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

public record InspectionClosed(InspectionId inspectionId, Asset asset, List<CriterionResponse<?>> nonConformities,
                               Person closedBy, Instant closedAt) {

    public InspectionClosed {
        nonConformities = List.copyOf(nonConformities);
    }
}
