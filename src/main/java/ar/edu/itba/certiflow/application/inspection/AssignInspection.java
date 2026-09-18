package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.application.schema.InspectionSchemaRepository;
import ar.edu.itba.certiflow.application.schema.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.AuditedInspectionGenerator;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionAssignment;
import ar.edu.itba.certiflow.domain.schema.InspectionSchema;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.util.Objects;

public final class AssignInspection {

    private final InspectionSchemaRepository schemas;
    private final InspectionRepository inspections;
    private final AuditedInspectionGenerator inspectionGenerator;
    private final Clock clock;

    public AssignInspection(InspectionSchemaRepository schemas, InspectionRepository inspections,
                            AuditedInspectionGenerator inspectionGenerator, Clock clock) {
        this.schemas = Objects.requireNonNull(schemas, "schemas");
        this.inspections = Objects.requireNonNull(inspections, "inspections");
        this.inspectionGenerator = Objects.requireNonNull(inspectionGenerator, "inspectionGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** La inspección queda atada a la versión vigente en este momento; versiones posteriores no la afectan. */
    public Inspection execute(Asset asset, InspectionAssignment assignment, Person assignedBy) {
        InspectionSchema schema = schemas.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()));
        Inspection inspection = inspectionGenerator.generate(asset, assignment, schema.currentVersion(), assignedBy,
                clock.instant());
        inspections.save(inspection);
        return inspection;
    }
}
