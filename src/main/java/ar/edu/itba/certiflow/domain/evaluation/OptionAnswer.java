package ar.edu.itba.certiflow.domain.evaluation;
public record OptionAnswer(String option) implements Answer {

    public OptionAnswer {
        if (option.isBlank()) {
            throw new IllegalArgumentException("An option answer must name the chosen option");
        }
    }
}
