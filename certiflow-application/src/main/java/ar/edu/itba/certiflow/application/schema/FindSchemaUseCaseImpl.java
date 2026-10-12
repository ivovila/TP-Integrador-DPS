package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.FindSchemaUseCase;

public final class FindSchemaUseCaseImpl implements FindSchemaUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;

    public FindSchemaUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
    }

    @Override
    public InspectionSchema execute(AssetType type) {
        return inspectionSchemaRepository.findByAssetType(type).orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(type));
    }
}
