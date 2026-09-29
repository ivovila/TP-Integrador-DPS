package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

public record InspectionClosed(InspectionView inspection, List<CriterionResponse<?>> nonConformities, Person closedBy,
                               Instant closedAt) {

    public InspectionClosed {
        nonConformities = List.copyOf(nonConformities);
    }

    InspectionClosed from(InspectionView source) {
        return new InspectionClosed(source, nonConformities, closedBy, closedAt);
    }
}
