package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class InspectionFactory {

    public Inspection create(InspectionId id, Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                             Person assignedBy, Instant assignedAt) {
        return new Inspection(id, asset, assignment, schemaVersion, assignedBy, assignedAt);
    }
}
