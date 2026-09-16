package ar.edu.itba.certiflow.domain;

import ar.edu.itba.certiflow.domain.rule.DocumentaryRule;
import ar.edu.itba.certiflow.domain.rule.EvaluationRule;

import java.util.Objects;
import java.util.Set;

public record Criterion(
        String code,
        String description,
        Severity severity,
        Set<Evidence.Kind> requiredEvidence,
        EvaluationRule rule) {
    public Criterion {
        code = Checks.text(code, "code");
        description = Checks.text(description, "description");
        Objects.requireNonNull(severity);
        requiredEvidence = Set.copyOf(requiredEvidence);
        Objects.requireNonNull(rule);
        Checks.require(
                !(rule instanceof DocumentaryRule)
                        || requiredEvidence.contains(Evidence.Kind.DOCUMENT),
                "Documentary criteria require document evidence");
    }

    public Evaluation evaluate(Submission submission) {
        if (submission == null) return new Evaluation(Outcome.INCOMPLETE, "No answer for " + code);
        Evaluation result = rule.evaluate(submission.answer());
        var missing =
                requiredEvidence.stream()
                        .filter(
                                kind ->
                                        submission.evidence().stream()
                                                .noneMatch(e -> e.kind() == kind))
                        .sorted()
                        .toList();
        return missing.isEmpty()
                ? result
                : new Evaluation(Outcome.INCOMPLETE, "Missing evidence: " + missing);
    }
}
