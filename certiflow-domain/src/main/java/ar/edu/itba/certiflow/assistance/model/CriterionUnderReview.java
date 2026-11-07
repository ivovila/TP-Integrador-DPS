package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;
import java.util.stream.Stream;

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
        return freeText().stream().anyMatch(quote::appearsIn);
    }

    /** Lo que el inspector escribió y la regla no lee: las observaciones y las descripciones de evidencia. */
    public List<String> freeText() {
        return Stream.concat(observations.stream(), evidence.stream().map(EvidenceUnderReview::description)).toList();
    }
}
