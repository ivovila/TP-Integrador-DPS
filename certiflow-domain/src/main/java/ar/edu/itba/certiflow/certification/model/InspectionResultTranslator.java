package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

/**
 * Lo único que la certificación conoce de una inspección es su identificador. Este puerto le trae, ya traducido
 * a su propio modelo, lo que necesita de ella.
 */
public interface InspectionResultTranslator {

    /** El activo de la inspección, sin cargarla: es lo que se bloquea antes de traducirla. */
    AssetCode assetCodeOf(InspectionId inspectionId);

    InspectionResult translate(InspectionId inspectionId);
}
