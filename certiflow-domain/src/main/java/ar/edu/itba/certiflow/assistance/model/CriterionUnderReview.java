package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;

/**
 * Un criterio tal como lo lee el asistente: la regla y la respuesta ya escritas en palabras, el resultado que
 * calculó la regla y el texto libre que la regla no lee. No lleva la severidad configurada: el modelo la estima.
 */
public record CriterionUnderReview(CriterionCode code, String text, String rule, String answer, Outcome outcome,
                                   List<String> observations, List<EvidenceUnderReview> evidence) {

    public CriterionUnderReview {
        observations = List.copyOf(observations);
        evidence = List.copyOf(evidence);
    }

    public boolean is(CriterionCode candidate) {
        return code.equals(candidate);
    }

    /** La regla ya levanta un hallazgo por este criterio al cerrar. */
    public boolean isNonConforming() {
        return outcome.raisesFinding();
    }

    public boolean mentions(Quote quote) {
        return observations.stream().anyMatch(quote::appearsIn)
                || evidence.stream().map(EvidenceUnderReview::description).anyMatch(quote::appearsIn);
    }
}
