package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record OptionAnswer(String option) implements Answer {

    public OptionAnswer {
        if (option.isBlank()) {
            throw new RequiredValueMissingException("An option answer must name the chosen option");
        }
    }

    @Override
    public <T> T describedBy(AnswerDescriber<T> describer) {
        return describer.option(option);
    }
}
