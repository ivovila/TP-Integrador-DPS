package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.ListSchemasUseCase;
import java.util.List;

public final class ListSchemasUseCaseImpl implements ListSchemasUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;

    public ListSchemasUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
    }

    @Override
    public List<InspectionSchema> execute() {
        return inspectionSchemaRepository.findAll();
    }
}
