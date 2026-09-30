package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.inspection.InspectionClosed;
import java.util.List;
import java.util.function.Supplier;

public final class FindingFactory {

    public List<Finding> create(InspectionClosed closure, Supplier<FindingId> nextId) {
        return closure.nonConformities().stream()
                .map(nonConformity -> new Finding(nextId.get(), closure.inspectionId(), closure.asset(), nonConformity,
                        closure.closedBy(), closure.closedAt()))
                .toList();
    }
}
