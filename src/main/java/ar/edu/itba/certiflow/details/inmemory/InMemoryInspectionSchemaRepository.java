package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class InMemoryInspectionSchemaRepository implements InspectionSchemaRepository {

    private final Set<InspectionSchema> schemas = new LinkedHashSet<>();

    @Override
    public void save(InspectionSchema schema) {
        schemas.add(schema);
    }

    @Override
    public Optional<InspectionSchema> findByAssetType(AssetType assetType) {
        return schemas.stream().filter(schema -> schema.appliesTo(assetType)).findFirst();
    }
}
