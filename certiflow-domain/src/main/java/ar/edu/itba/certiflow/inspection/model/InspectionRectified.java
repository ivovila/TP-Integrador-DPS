package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

/** Hecho de dominio: una rectificación cerrada, con las no conformidades que la respuesta nueva introduce. */
public record InspectionRectified(InspectionId inspectionId, AssetCode assetCode, Person assetResponsible,
                                  List<CriterionResponse<?>> nonConformities, Person rectifiedBy, Instant rectifiedAt, SchemaVersion schemaVersion) {

    public InspectionRectified {
        nonConformities = List.copyOf(nonConformities);
    }
}
