package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.FindEffectiveSchemaVersionUseCase;
import java.time.Instant;

public final class FindEffectiveSchemaVersionUseCaseImpl implements FindEffectiveSchemaVersionUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;

    public FindEffectiveSchemaVersionUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
    }

    @Override
    public SchemaVersion execute(AssetType assetType, Instant at) {
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(assetType)
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(assetType));
        return schema.versionAt(at);
    }
}
