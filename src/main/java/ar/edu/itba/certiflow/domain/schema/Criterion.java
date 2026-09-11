package ar.edu.itba.certiflow.domain.schema;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import ar.edu.itba.certiflow.domain.shared.Answer;
import ar.edu.itba.certiflow.domain.shared.Measurement;

public record Criterion(CriterionId id, String description, Severity severity, Set<EvidenceType> requiredEvidence, List<ApprovalRule> approvalRules) {
    public Criterion {
        requiredEvidence = Set.copyOf(requiredEvidence);
        approvalRules = List.copyOf(approvalRules);
    }
    public boolean expects(Measurement measurement) {
        return approvalRules.stream().anyMatch(rule -> rule.accepts(measurement));
    }

    public CriterionOutcome evaluate(List<Answer> answers, List<Measurement> measurements) {
        return approvalRules.stream()
                .map(rule -> rule.evaluate(answers, measurements))
                .max(Comparator.naturalOrder())
                .orElse(CriterionOutcome.APPROVED);
    }
}
