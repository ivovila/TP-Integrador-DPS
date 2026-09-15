package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;

public interface InspectionRepository {

    Optional<Inspection> findById(InspectionId id);

    void save(Inspection inspection);
}
