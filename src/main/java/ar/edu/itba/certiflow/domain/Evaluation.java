package ar.edu.itba.certiflow.domain;

import java.util.Objects;

public record Evaluation(Outcome outcome, String explanation) {
    public Evaluation {
        Objects.requireNonNull(outcome);
        explanation = Checks.text(explanation, "explanation");
    }

    public boolean isFinding() {
        return outcome == Outcome.OBSERVED || outcome == Outcome.REJECTED;
    }
}
