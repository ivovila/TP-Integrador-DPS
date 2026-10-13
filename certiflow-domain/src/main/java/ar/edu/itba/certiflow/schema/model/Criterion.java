package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.NoSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;

public record Criterion<A extends Answer>(CriterionCode code, String text, ApprovalRule<A> rule, Severity severity,
                                          List<EvidenceRequirement> requiredEvidence, SubsystemAssociation subsystem) {

    public Criterion(CriterionCode code, String text, ApprovalRule<A> rule, Severity severity,
                     List<EvidenceRequirement> requiredEvidence) {
        this(code, text, rule, severity, requiredEvidence, new NoSubsystem());
    }

    public Criterion {
        requiredEvidence = List.copyOf(requiredEvidence);
        if (text.isBlank()) {
            throw new InvalidSchemaException("A criterion needs a text");
        }
        if (requiredEvidence.stream().map(EvidenceRequirement::kind).distinct().count() < requiredEvidence.size()) {
            throw new InvalidSchemaException("Criterion " + code.value() + " requires the same evidence kind more than once");
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
