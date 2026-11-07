package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import java.util.List;

/**
 * Lo que propuso el modelo, antes de los guardrails. La severidad es la que estimó leyendo el texto: la que vale
 * si termina habiendo hallazgo es la del criterio.
 */
public record ProposedFinding(CriterionCode criterion, Severity estimatedSeverity, String rationale, List<Quote> quotes) {

    public ProposedFinding {
        quotes = List.copyOf(quotes);
    }
}
