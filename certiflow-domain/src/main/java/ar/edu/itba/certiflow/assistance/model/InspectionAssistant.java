package ar.edu.itba.certiflow.assistance.model;

/**
 * El asistente de inspección: el contrato propio que pide la consigna, del que no depende ningún proveedor.
 * Quien lo implementa solo devuelve propuestas; no ve el agregado ni tiene cómo cambiar nada. Si el modelo no
 * responde, o responde algo que no cumple el formato acordado, lanza {@code AssistantUnavailableException}.
 */
public interface InspectionAssistant {

    AssistantReply review(InspectionUnderReview inspection);
}
