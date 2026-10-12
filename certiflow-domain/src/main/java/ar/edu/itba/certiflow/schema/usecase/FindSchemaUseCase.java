package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;

public interface FindSchemaUseCase {

    InspectionSchema execute(AssetType type);
}
