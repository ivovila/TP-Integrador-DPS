package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionCommand;
import ar.edu.itba.certiflow.schema.usecase.PublishSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.Clock;
import java.time.Instant;

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
        InspectionSchema schema = inspectionSchemaRepository.findForUpdate(command.assetType())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(command.assetType()));
        Instant publishedAt = StoredInstant.normalize(clock.instant());
        InspectionSchema updatedSchema = inspectionSchemaService.addVersion(schema, command.sections(), command.publishedBy(),
                publishedAt, command.effectiveFrom().orElse(publishedAt), command.requiredSubsystems());
        inspectionSchemaRepository.save(updatedSchema);
        return updatedSchema.latestPublishedVersion();
    }
}
