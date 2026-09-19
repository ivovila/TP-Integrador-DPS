package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.AssetType;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import java.util.Optional;

public interface InspectionSchemaRepository {

    void save(InspectionSchema schema);

    Optional<InspectionSchema> findByAssetType(AssetType assetType);
}
