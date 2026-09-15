package ar.edu.itba.certiflow.domain.model.inspection;

import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;

public interface InspectionState {

    default InspectionState start() {
        throw new InvalidTransitionException("La inspeccion no se puede iniciar en este estado");
    }

    default void checkCanRegister() {
        throw new InvalidTransitionException("La inspeccion no admite registros en este estado");
    }

    default InspectionState close() {
        throw new InvalidTransitionException("La inspeccion no se puede cerrar en este estado");
    }

    default InspectionState rectify() {
        throw new InvalidTransitionException("Solo se puede rectificar una inspeccion cerrada");
    }

    default void checkCanEvaluate() {
        throw new InvalidTransitionException("La inspeccion se evalua una vez cerrada");
    }
}
