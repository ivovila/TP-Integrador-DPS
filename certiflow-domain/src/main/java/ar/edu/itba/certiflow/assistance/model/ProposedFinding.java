package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.List;

/**
 * Lo que propuso el modelo, antes de los guardrails. La severidad es la que estimó leyendo el texto: la que vale
 * si termina habiendo hallazgo es la del criterio. Una propuesta sin fundamento no existe: el adaptador que recibe
 * una respuesta sin él la rechaza entera como fuera de formato.
 */
public record ProposedFinding(CriterionCode criterion, Severity estimatedSeverity, String rationale, List<Quote> quotes) {

    public ProposedFinding {
        if (rationale.isBlank()) {
            throw new RequiredValueMissingException("A proposed finding must explain its rationale");
        }
        quotes = List.copyOf(quotes);
    }
}
