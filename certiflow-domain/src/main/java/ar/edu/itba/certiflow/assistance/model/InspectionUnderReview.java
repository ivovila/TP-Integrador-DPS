package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.assistance.model.exceptions.ReviewOfClosedInspectionException;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import java.util.List;

/**
 * Lo que el asistente puede leer de una inspección, armado por el traductor: ni el agregado ni quién escribió qué.
 * Es también lo que responden los guardrails, porque es exactamente lo que el modelo tuvo a la vista.
 */
public record InspectionUnderReview(InspectionId inspectionId, AssetType assetType, int schemaVersion, boolean closed,
                                    List<CriterionUnderReview> criteria) {

    public InspectionUnderReview {
        criteria = List.copyOf(criteria);
    }

    public void ensureOpen() {
        if (closed) {
            throw new ReviewOfClosedInspectionException(inspectionId);
        }
    }

    public boolean includes(CriterionCode code) {
        return criteria.stream().anyMatch(criterion -> criterion.is(code));
    }

    public boolean isNonConforming(CriterionCode code) {
        return criteria.stream().filter(criterion -> criterion.is(code)).anyMatch(CriterionUnderReview::isNonConforming);
    }

    /** La cita puede venir de cualquier criterio: un inspector escribe donde le queda cómodo. */
    public boolean mentions(Quote quote) {
        return criteria.stream().anyMatch(criterion -> criterion.mentions(quote));
    }
}
