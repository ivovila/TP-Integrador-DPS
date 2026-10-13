package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaCommand;
import ar.edu.itba.certiflow.schema.usecase.CreateInspectionSchemaUseCase;
import java.time.Clock;

public final class CreateInspectionSchemaUseCaseImpl implements CreateInspectionSchemaUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public CreateInspectionSchemaUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
                                            Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    @Override
    public InspectionSchema execute(CreateInspectionSchemaCommand command) {
        if (inspectionSchemaRepository.findByAssetType(command.assetType()).isPresent()) {
            throw new SchemaAlreadyDefinedForAssetTypeException(command.assetType());
        }
        InspectionSchema schema = inspectionSchemaService.create(command.name(), command.assetType(), command.sections(),
                new Stamp(command.createdBy(), clock.instant()), command.requiredSubsystems());
        inspectionSchemaRepository.save(schema);
        return schema;
    }
}
