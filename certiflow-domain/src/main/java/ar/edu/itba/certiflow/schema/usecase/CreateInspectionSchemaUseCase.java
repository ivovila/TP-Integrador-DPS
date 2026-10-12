package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;

public interface CreateInspectionSchemaUseCase {

    InspectionSchema execute(CreateInspectionSchemaCommand command);
}
