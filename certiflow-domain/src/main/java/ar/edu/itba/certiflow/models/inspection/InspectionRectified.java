package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

/** Hecho de dominio: una rectificación cerrada, con las no conformidades que la respuesta nueva introduce. */
public record InspectionRectified(InspectionId inspectionId, AssetCode assetCode, Person assetResponsible,
                                  List<CriterionResponse<?>> nonConformities, Person rectifiedBy, Instant rectifiedAt) {

    public InspectionRectified {
        nonConformities = List.copyOf(nonConformities);
    }
}
