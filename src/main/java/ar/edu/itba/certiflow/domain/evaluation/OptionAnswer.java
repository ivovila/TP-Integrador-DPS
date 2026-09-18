package ar.edu.itba.certiflow.domain.evaluation;

import java.util.Objects;

public record OptionAnswer(String option) implements Answer {

    public OptionAnswer {
        Objects.requireNonNull(option, "option");
        if (option.isBlank()) {
            throw new IllegalArgumentException("An option answer must name the chosen option");
        }
    }
}
