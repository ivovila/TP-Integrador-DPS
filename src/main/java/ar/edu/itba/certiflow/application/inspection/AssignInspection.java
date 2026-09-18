package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.application.schema.InspectionSchemaRepository;
import ar.edu.itba.certiflow.application.schema.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.domain.inspection.InspectionGenerator;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;

public final class AssignInspection {

    private final InspectionSchemaRepository schemas;
    private final InspectionRepository inspections;
    private final InspectionGenerator inspectionGenerator;
    private final Clock clock;

    public AssignInspection(InspectionSchemaRepository schemas, InspectionRepository inspections,
                            InspectionGenerator inspectionGenerator, Clock clock) {
        this.schemas = schemas;
        this.inspections = inspections;
        this.inspectionGenerator = inspectionGenerator;
        this.clock = clock;
    }

    public Inspection execute(Asset asset, InspectionAssignment assignment, Person assignedBy) {
        InspectionSchema schema = schemas.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionGenerator.generate(asset, assignment, schema.currentVersion(), assignedBy,
                clock.instant());
        inspections.save(inspection);
        return inspection;
    }
}
