package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

public record InspectionClosed(InspectionId inspectionId, AssetCode assetCode, Person assetResponsible,
                               List<CriterionResponse<?>> nonConformities, Person closedBy, Instant closedAt, SchemaVersion schemaVersion) {

    public InspectionClosed {
        nonConformities = List.copyOf(nonConformities);
    }
}
