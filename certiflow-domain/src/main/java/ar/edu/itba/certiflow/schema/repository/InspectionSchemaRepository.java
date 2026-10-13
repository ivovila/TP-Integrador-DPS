package ar.edu.itba.certiflow.schema.repository;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import java.util.Optional;

public interface InspectionSchemaRepository {

    void save(InspectionSchema schema);

    default Optional<InspectionSchema> findForUpdate(AssetType assetType) {
        return findByAssetType(assetType);
    }

    Optional<InspectionSchema> findByAssetType(AssetType assetType);
}
