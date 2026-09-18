package ar.edu.itba.certiflow.details.evaluation;

import ar.edu.itba.certiflow.domain.evaluation.Answer;

import java.util.Objects;

public record OptionAnswer(String option) implements Answer {

    public OptionAnswer {
        Objects.requireNonNull(option, "option");
        if (option.isBlank()) {
            throw new IllegalArgumentException("An option answer must name the chosen option");
        }
    }
}
