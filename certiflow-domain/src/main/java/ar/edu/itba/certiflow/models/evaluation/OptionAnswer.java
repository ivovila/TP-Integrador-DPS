package ar.edu.itba.certiflow.models.evaluation;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;
public record OptionAnswer(String option) implements Answer {

    public OptionAnswer {
        if (option.isBlank()) {
            throw new RequiredValueMissingException("An option answer must name the chosen option");
        }
    }
}
