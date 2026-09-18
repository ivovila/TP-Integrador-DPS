package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.application.inspection.InspectionRepository;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class InMemoryInspectionRepository implements InspectionRepository {

    private final Set<Inspection> inspections = new LinkedHashSet<>();

    @Override
    public void save(Inspection inspection) {
        inspections.add(inspection);
    }

    @Override
    public List<Inspection> findByAsset(Asset asset) {
        return inspections.stream().filter(inspection -> inspection.asset().equals(asset)).toList();
    }
}
