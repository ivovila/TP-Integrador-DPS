package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.FindEffectiveSchemaVersionUseCase;
import java.time.Instant;

public final class FindEffectiveSchemaVersionUseCaseImpl implements FindEffectiveSchemaVersionUseCase {
    private final InspectionSchemaRepository schemas;
    public FindEffectiveSchemaVersionUseCaseImpl(InspectionSchemaRepository schemas) { this.schemas = schemas; }
    @Override public SchemaVersion execute(AssetType type, Instant at) {
        return schemas.findByAssetType(type).orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(type)).versionAt(at);
    }
}
