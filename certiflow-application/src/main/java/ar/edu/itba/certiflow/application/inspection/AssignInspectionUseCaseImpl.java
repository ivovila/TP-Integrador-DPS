package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionCommand;
import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.inspection.usecase.AssignInspectionUseCase;
import java.time.Clock;

public final class AssignInspectionUseCaseImpl implements AssignInspectionUseCase {

    private final AssetRepository assetRepository;
    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public AssignInspectionUseCaseImpl(AssetRepository assetRepository, InspectionSchemaRepository inspectionSchemaRepository,
                                      InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.assetRepository = assetRepository;
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public InspectionId execute(AssignInspectionCommand command) {
        Asset asset = assetRepository.findByCode(command.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(command.assetCode()));
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionService.assign(inspectionRepository.nextId(), asset, command.assignment(),
                schema.currentVersion(), command.assignedBy(), clock.instant());
        inspectionRepository.save(inspection);
        return inspection.id();
    }
}
