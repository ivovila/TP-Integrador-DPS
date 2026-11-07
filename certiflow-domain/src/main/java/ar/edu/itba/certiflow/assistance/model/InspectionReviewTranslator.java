package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

/**
 * Lo único que la asistencia conoce de una inspección es su identificador. Este puerto le trae, ya traducido a su
 * propio modelo, lo que el asistente puede leer de ella.
 */
public interface InspectionReviewTranslator {

    /** El activo de la inspección, sin cargarla: es lo que se bloquea antes de leerla para guardar una revisión. */
    AssetCode assetCodeOf(InspectionId inspectionId);

    InspectionUnderReview translate(InspectionId inspectionId);
}
