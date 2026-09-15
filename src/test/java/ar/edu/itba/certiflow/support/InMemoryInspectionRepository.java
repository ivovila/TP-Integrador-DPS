package ar.edu.itba.certiflow.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.ports.InspectionRepository;

public class InMemoryInspectionRepository implements InspectionRepository {

    private final Map<InspectionId, Inspection> inspections = new HashMap<>();

    @Override
    public Optional<Inspection> findById(InspectionId id) {
        return Optional.ofNullable(inspections.get(id));
    }

    @Override
    public void save(Inspection inspection) {
        inspections.put(inspection.getId(), inspection);
    }
}
