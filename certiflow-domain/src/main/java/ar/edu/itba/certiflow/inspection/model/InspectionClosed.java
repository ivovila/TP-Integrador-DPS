package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;

public record InspectionClosed(InspectionId inspectionId, AssetCode assetCode, Person assetResponsible,
                               List<CriterionResponse<?>> nonConformities, Stamp closed, SchemaVersion schemaVersion)
        implements RaisesFindings {

    public InspectionClosed {
        nonConformities = List.copyOf(nonConformities);
    }

    @Override
    public Stamp raised() {
        return closed;
    }
}
