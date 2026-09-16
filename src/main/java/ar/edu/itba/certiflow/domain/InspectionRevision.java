package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Preserved closed revision before a rectification. */
public record InspectionRevision(
        int number,
        Map<String, Submission> submissions,
        Map<String, Evaluation> evaluations,
        List<Finding> findings,
        Instant closedAt) {
    public InspectionRevision {
        Checks.require(number > 0, "Invalid revision");
        submissions = Map.copyOf(submissions);
        evaluations = Map.copyOf(evaluations);
        findings = List.copyOf(findings);
        Objects.requireNonNull(closedAt);
    }
}
