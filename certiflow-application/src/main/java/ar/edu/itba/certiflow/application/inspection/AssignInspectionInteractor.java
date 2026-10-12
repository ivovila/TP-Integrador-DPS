package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.asset.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import ar.edu.itba.certiflow.usecases.inspection.AssignInspection;
import java.time.Clock;

public final class AssignInspectionInteractor implements AssignInspection {

    private final AssetRepository assetRepository;
    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public AssignInspectionInteractor(AssetRepository assetRepository, InspectionSchemaRepository inspectionSchemaRepository,
                                      InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.assetRepository = assetRepository;
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public InspectionId execute(AssetCode assetCode, InspectionAssignment assignment, Person assignedBy) {
        Asset asset = assetRepository.findByCode(assetCode).orElseThrow(() -> new AssetNotFoundException(assetCode));
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionService.assign(inspectionRepository.nextId(), asset, assignment,
                schema.currentVersion(), assignedBy, clock.instant());
        inspectionRepository.save(inspection);
        return inspection.id();
    }
}
