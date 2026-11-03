package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.InspectionSchemaService;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.schema.usecase.CancelSchemaVersionCommand;
import ar.edu.itba.certiflow.schema.usecase.CancelSchemaVersionUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;

public final class CancelSchemaVersionUseCaseImpl implements CancelSchemaVersionUseCase {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionSchemaService inspectionSchemaService;
    private final Clock clock;

    public CancelSchemaVersionUseCaseImpl(InspectionSchemaRepository inspectionSchemaRepository,
                                          InspectionSchemaService inspectionSchemaService, Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionSchemaService = inspectionSchemaService;
        this.clock = clock;
    }

    @Override
    public SchemaVersion execute(CancelSchemaVersionCommand command) {
        InspectionSchema schema = inspectionSchemaRepository.findForUpdate(command.assetType())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(command.assetType()));
        InspectionSchema updatedSchema = inspectionSchemaService.cancelVersion(schema, command.versionNumber(),
                new Stamp(command.cancelledBy(), clock.instant()), command.reason());
        inspectionSchemaRepository.save(updatedSchema);
        return updatedSchema.version(command.versionNumber());
    }
}
