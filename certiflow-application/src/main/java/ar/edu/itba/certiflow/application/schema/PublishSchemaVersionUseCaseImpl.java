package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import java.time.Clock;

public final class PublishSchemaVersionUseCaseImpl implements PublishSchemaVersionUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public PublishSchemaVersionUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository, InspectionSchemaService inspectionSchemaService,
                                          Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    @Override
    public SchemaVersion execute(PublishSchemaVersionCommand command) {
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(command.assetType())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(command.assetType()));
        InspectionSchema updatedSchema = inspectionSchemaService.addVersion(schema, command.sections(), command.publishedBy(),
                clock.instant());
        inspectionSchemaRepository.save(updatedSchema);
        return updatedSchema.currentVersion();
    }
}
