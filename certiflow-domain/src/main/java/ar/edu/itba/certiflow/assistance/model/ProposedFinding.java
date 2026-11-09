package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import java.util.List;
import java.util.Objects;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

/**
 * Lo que propuso el modelo, antes de los guardrails. La severidad es la que estimó leyendo el texto: la que vale
 * si termina habiendo hallazgo es la del criterio.
 */
public record ProposedFinding(CriterionCode criterion, Severity estimatedSeverity, String rationale, List<Quote> quotes) {

    public ProposedFinding {
        Objects.requireNonNull(criterion, "criterion");
        Objects.requireNonNull(estimatedSeverity, "estimatedSeverity");
        if (rationale == null || rationale.isBlank()) {
            throw new RequiredValueMissingException("A proposed finding must explain its rationale");
        }
        quotes = List.copyOf(quotes);
    }
}
