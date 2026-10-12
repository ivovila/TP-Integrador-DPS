package ar.edu.itba.certiflow.inspection.repository;

import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.Optional;

public interface InspectionRepository {

    InspectionId nextId();

    void save(Inspection inspection);

    Optional<Inspection> findById(InspectionId id);
}
