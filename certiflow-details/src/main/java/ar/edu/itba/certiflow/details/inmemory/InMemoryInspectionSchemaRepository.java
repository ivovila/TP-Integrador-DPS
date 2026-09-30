package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
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
