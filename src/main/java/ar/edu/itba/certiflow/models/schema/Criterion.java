package ar.edu.itba.certiflow.models.schema;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.evaluation.ApprovalRule;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.evaluation.EvidenceRequirement;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.schema.exceptions.InvalidSchemaException;
import java.util.List;

public record Criterion<A extends Answer>(String code, String text, ApprovalRule<A> rule, Severity severity,
                                          List<EvidenceRequirement> requiredEvidence) {

    public Criterion {
        requiredEvidence = List.copyOf(requiredEvidence);
        if (code.isBlank() || text.isBlank()) {
            throw new InvalidSchemaException("A criterion needs a code and a text");
        }
    }

    public Outcome outcomeOf(A answer) {
        return rule.isSatisfiedBy(answer) ? Outcome.APPROVED : severity.outcomeWhenUnmet();
    }

    public boolean evidenceRequirementsMetBy(List<Evidence> attachedEvidence) {
        return requiredEvidence.stream().allMatch(requirement -> requirement.isSatisfiedBy(attachedEvidence));
    }
}
