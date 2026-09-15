package ar.edu.itba.certiflow.domain.model.inspection;

import ar.edu.itba.certiflow.domain.model.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;

public interface InspectionState {

    default InspectionState start(SchemaVersion schema) {
        throw new InvalidTransitionException("La inspeccion no se puede iniciar en este estado");
    }

    default void checkCanRegister() {
        throw new InvalidTransitionException("La inspeccion no admite registros en este estado");
    }

    default InspectionState close(InspectionEvaluation evaluation) {
        throw new InvalidTransitionException("La inspeccion no se puede cerrar en este estado");
    }

    default InspectionState rectify(InspectionEvaluation evaluation) {
        throw new InvalidTransitionException("Solo se puede rectificar una inspeccion cerrada");
    }

    default SchemaVersion schema() {
        throw new InvalidTransitionException("La inspeccion todavia no fue iniciada");
    }

    default InspectionEvaluation evaluation() {
        throw new InvalidTransitionException("La inspeccion todavia no fue cerrada");
    }
}
