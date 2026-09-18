package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.domain.asset.AssetType;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import java.util.Optional;

public interface InspectionSchemaRepository {

    void save(InspectionSchema schema);

    Optional<InspectionSchema> findByAssetType(AssetType assetType);
}
