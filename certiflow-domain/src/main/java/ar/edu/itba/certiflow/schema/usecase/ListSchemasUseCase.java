package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;

public interface ListSchemasUseCase {

    Page<InspectionSchema> execute(PageRequest request);
}
