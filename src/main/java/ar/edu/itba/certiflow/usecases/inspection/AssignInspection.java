package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.asset.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.models.inspection.AuditedInspectionFactory;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.schema.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;

public final class AssignInspection {

    private final AssetRepository assetRepository;
    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionRepository inspectionRepository;
    private final AuditedInspectionFactory inspectionFactory;
    private final Clock clock;

    public AssignInspection(AssetRepository assetRepository, InspectionSchemaRepository inspectionSchemaRepository,
                            InspectionRepository inspectionRepository, AuditedInspectionFactory inspectionFactory, Clock clock) {
        this.assetRepository = assetRepository;
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionFactory = inspectionFactory;
        this.clock = clock;
    }

    public InspectionId execute(AssetCode assetCode, InspectionAssignment assignment, Person assignedBy) {
        Asset asset = assetRepository.findByCode(assetCode).orElseThrow(() -> new AssetNotFoundException(assetCode));
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionFactory.create(inspectionRepository.nextId(), asset, assignment, schema.currentVersion(),
                assignedBy, clock.instant());
        inspectionRepository.save(inspection);
        return inspection.id();
    }
}
