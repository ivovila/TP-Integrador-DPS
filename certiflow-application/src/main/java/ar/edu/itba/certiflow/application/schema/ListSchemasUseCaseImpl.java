package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.ListSchemasUseCase;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;

public final class ListSchemasUseCaseImpl implements ListSchemasUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;

    public ListSchemasUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
    }

    @Override
    public Page<InspectionSchema> execute(PageRequest request) {
        return inspectionSchemaRepository.findPage(request);
    }
}
