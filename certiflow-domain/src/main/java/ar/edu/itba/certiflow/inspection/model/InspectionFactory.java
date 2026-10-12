package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public final class InspectionFactory {

    public Inspection create(InspectionId id, Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion,
                             Person assignedBy, Instant assignedAt) {
        return new Inspection(id, asset, assignment, schemaVersion, assignedBy, assignedAt);
    }

    public Inspection restore(InspectionSnapshot snapshot) {
        return new Inspection(snapshot);
    }
}
