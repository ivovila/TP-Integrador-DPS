package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.ApprovalRule;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.EvidenceRequirement;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.schema.exceptions.InvalidSchemaException;

import java.util.List;
import java.util.Objects;

/**
 * El tipo de respuesta que admite un criterio es el de su regla. El código es el que muestran
 * las planillas y el acta; no es un identificador técnico.
 */
public record Criterion<A extends Answer>(String code, String text, ApprovalRule<A> rule, Severity severity,
                                          List<EvidenceRequirement> requiredEvidence) {

    public Criterion {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(rule, "rule");
        Objects.requireNonNull(severity, "severity");
        requiredEvidence = List.copyOf(requiredEvidence);
        if (code.isBlank() || text.isBlank()) {
            throw new InvalidSchemaException("A criterion needs a code and a text");
        }
    }

    public Outcome outcomeOf(A answer) {
        return rule.isSatisfiedBy(answer) ? Outcome.APPROVED : severity.outcomeWhenUnmet();
    }

    public boolean evidenceRequirementsMetBy(List<Evidence> attached) {
        return requiredEvidence.stream().allMatch(requirement -> requirement.isSatisfiedBy(attached));
    }
}
