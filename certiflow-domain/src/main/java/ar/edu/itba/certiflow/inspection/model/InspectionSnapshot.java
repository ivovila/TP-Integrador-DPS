package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;

public record InspectionSnapshot(InspectionId id, AssetCode assetCode, Person assetResponsible, InspectionAssignment assignment,
                                 SchemaVersion schemaVersion, List<CriterionResponse<?>> responses, Revisions revisions) {

    public InspectionSnapshot {
        responses = List.copyOf(responses);
    }
}
