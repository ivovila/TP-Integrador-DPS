package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;

public record Criterion<A extends Answer>(String code, String text, ApprovalRule<A> rule, Severity severity,
                                          List<EvidenceRequirement> requiredEvidence) {

    public Criterion {
        requiredEvidence = List.copyOf(requiredEvidence);
        if (code.isBlank() || text.isBlank()) {
            throw new InvalidSchemaException("A criterion needs a code and a text");
        }
        if (requiredEvidence.stream().map(EvidenceRequirement::kind).distinct().count() < requiredEvidence.size()) {
            throw new InvalidSchemaException("Criterion " + code + " requires the same evidence kind more than once");
        }
    }

    public Outcome outcomeOf(A answer) {
        return rule.isSatisfiedBy(answer) ? Outcome.APPROVED : severity.outcomeWhenUnmet();
    }

    public boolean evidenceRequirementsMetBy(List<Evidence> attachedEvidence) {
        return requiredEvidence.stream().allMatch(requirement -> requirement.isSatisfiedBy(attachedEvidence));
    }
}
