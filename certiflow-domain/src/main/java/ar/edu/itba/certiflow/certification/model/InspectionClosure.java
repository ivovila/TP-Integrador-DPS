package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.Optional;

/** Lo que la certificación sabe del cierre de la inspección que respalda la decisión. */
public interface InspectionClosure {

    boolean isClosed();

    Optional<InspectionReference> referenceOf(InspectionId inspectionId);
}
