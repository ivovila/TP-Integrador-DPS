package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.application.schema.InspectionSchemaRepository;
import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
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
