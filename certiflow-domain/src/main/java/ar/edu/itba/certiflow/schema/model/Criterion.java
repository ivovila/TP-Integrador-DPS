package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;
import java.util.Optional;

public record Criterion<A extends Answer>(String code, String text, ApprovalRule<A> rule, Severity severity,
                                          List<EvidenceRequirement> requiredEvidence, Optional<SubsystemCode> subsystemCode) {

    public Criterion(String code, String text, ApprovalRule<A> rule, Severity severity, List<EvidenceRequirement> requiredEvidence) {
        this(code, text, rule, severity, requiredEvidence, Optional.empty());
    }

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

    public A answerFrom(RawAnswer raw) {
        return rule.answerFrom(raw);
    }

    public boolean evidenceRequirementsMetBy(List<Evidence> attachedEvidence) {
        return requiredEvidence.stream().allMatch(requirement -> requirement.isSatisfiedBy(attachedEvidence));
    }
}
