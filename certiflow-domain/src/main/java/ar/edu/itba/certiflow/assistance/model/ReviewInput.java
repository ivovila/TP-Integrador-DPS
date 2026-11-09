package ar.edu.itba.certiflow.assistance.model;

/** Entrada exacta enviada al asistente, o ausencia explícita para revisiones previas a su captura. */
public interface ReviewInput {
    <T> T describedBy(ReviewInputDescriber<T> describer);
}
