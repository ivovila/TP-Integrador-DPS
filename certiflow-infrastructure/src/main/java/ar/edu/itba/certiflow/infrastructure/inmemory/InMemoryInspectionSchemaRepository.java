package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryInspectionSchemaRepository implements InspectionSchemaRepository {

    private final Map<AssetType, InspectionSchema> schemas = new LinkedHashMap<>();

    @Override
    public void save(InspectionSchema schema) {
        schemas.put(schema.assetType(), schema);
    }

    @Override
    public Optional<InspectionSchema> findByAssetType(AssetType assetType) {
        return Optional.ofNullable(schemas.get(assetType));
    }
}
