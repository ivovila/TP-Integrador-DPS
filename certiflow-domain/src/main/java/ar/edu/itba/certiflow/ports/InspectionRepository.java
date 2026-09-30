package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import java.util.Optional;

public interface InspectionRepository {

    InspectionId nextId();

    void save(Inspection inspection);

    Optional<Inspection> findById(InspectionId id);
}
