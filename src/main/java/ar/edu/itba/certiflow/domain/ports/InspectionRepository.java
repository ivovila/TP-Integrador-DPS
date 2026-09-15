package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;

public interface InspectionRepository {

    Optional<Inspection> findById(InspectionId id);

    default Inspection getById(InspectionId id) {
        return findById(id).orElseThrow(() -> new NotFoundException("la inspeccion", id.value()));
    }

    void save(Inspection inspection);
}
