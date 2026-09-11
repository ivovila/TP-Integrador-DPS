package ar.edu.itba.certiflow.domain.inspection;

public interface InspectionState {

    default InspectionState start() {
        throw new IllegalStateException("La inspeccion no se puede iniciar en este estado");
    }

    default void checkCanRegister() {
        throw new IllegalStateException("La inspeccion no admite registros en este estado");
    }

    default InspectionState close() {
        throw new IllegalStateException("La inspeccion no se puede cerrar en este estado");
    }
}
