package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;

public interface SchemaRepository {

    Optional<InspectionSchema> findById(SchemaId id);

    default InspectionSchema getById(SchemaId id) {
        return findById(id).orElseThrow(() -> new NotFoundException("el esquema", id.value()));
    }

    void save(InspectionSchema schema);
}
