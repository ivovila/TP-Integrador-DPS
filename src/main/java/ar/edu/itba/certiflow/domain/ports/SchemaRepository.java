package ar.edu.itba.certiflow.domain.ports;

import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;

public interface SchemaRepository {

    Optional<InspectionSchema> findById(SchemaId id);

    void save(InspectionSchema schema);
}
