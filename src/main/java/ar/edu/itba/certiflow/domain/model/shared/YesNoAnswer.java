package ar.edu.itba.certiflow.domain.model.shared;

public record YesNoAnswer(String question, boolean value) implements Answer {
}
