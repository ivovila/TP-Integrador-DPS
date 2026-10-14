package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;

/** Lo que la certificación sabe del cierre de la inspección que respalda la decisión. */
public interface InspectionClosure {

    boolean isClosed();

    /** La referencia que queda en el certificado. Una inspección abierta no respalda ninguno. */
    InspectionReference referenceOf(InspectionId inspectionId);
}
