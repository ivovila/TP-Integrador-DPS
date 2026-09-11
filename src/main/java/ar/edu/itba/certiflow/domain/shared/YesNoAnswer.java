package ar.edu.itba.certiflow.domain.shared;

public record YesNoAnswer(String question, boolean value) implements Answer {
}
