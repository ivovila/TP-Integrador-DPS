package ar.edu.itba.certiflow.domain;

import java.util.List;
import java.util.Objects;

/** Answer plus supporting material for one criterion. */
public record Submission(Answer answer, List<Evidence> evidence, String observations) {
    public Submission {
        Objects.requireNonNull(answer);
        evidence = List.copyOf(evidence);
        Objects.requireNonNull(observations);
        Checks.require(
                evidence.stream().map(Evidence::id).distinct().count() == evidence.size(),
                "Duplicate evidence");
    }
}
