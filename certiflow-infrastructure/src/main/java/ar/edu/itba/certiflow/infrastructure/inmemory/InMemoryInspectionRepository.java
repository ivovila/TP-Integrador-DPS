package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryInspectionRepository implements InspectionRepository {

    private final Map<InspectionId, Inspection> inspections = new LinkedHashMap<>();

    @Override
    public InspectionId nextId() {
        return new InspectionId(UUID.randomUUID().toString());
    }

    @Override
    public void save(Inspection inspection) {
        inspections.put(inspection.id(), inspection);
    }

    @Override
    public Optional<Inspection> findById(InspectionId id) {
        return Optional.ofNullable(inspections.get(id));
    }
}
