package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.application.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.inspection.AuditedInspectionFactory;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.ports.InspectionSchemaRepository;
import java.time.Clock;

public final class AssignInspection {

    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final InspectionRepository inspectionRepository;
    private final AuditedInspectionFactory inspectionFactory;
    private final Clock clock;

    public AssignInspection(InspectionSchemaRepository inspectionSchemaRepository, InspectionRepository inspectionRepository,
                            AuditedInspectionFactory inspectionFactory, Clock clock) {
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.inspectionRepository = inspectionRepository;
        this.inspectionFactory = inspectionFactory;
        this.clock = clock;
    }

    public Inspection execute(Asset asset, InspectionAssignment assignment, Person assignedBy) {
        InspectionSchema schema = inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionFactory.create(asset, assignment, schema.currentVersion(), assignedBy,
                clock.instant());
        inspectionRepository.save(inspection);
        return inspection;
    }
}
