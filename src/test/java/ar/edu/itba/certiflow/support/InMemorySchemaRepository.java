package ar.edu.itba.certiflow.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.model.schema.SchemaId;
import ar.edu.itba.certiflow.domain.ports.SchemaRepository;

public class InMemorySchemaRepository implements SchemaRepository {

    private final Map<SchemaId, InspectionSchema> schemas = new HashMap<>();

    @Override
    public Optional<InspectionSchema> findById(SchemaId id) {
        return Optional.ofNullable(schemas.get(id));
    }

    @Override
    public void save(InspectionSchema schema) {
        schemas.put(schema.getId(), schema);
    }
}
