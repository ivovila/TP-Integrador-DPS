package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

public record ActLine(String criterionCode, String criterionText, Answer answer, Person answeredBy,
                      Instant answeredAt, int evidenceCount, List<String> observations, Outcome outcome) {

    public ActLine {
        observations = List.copyOf(observations);
    }
}
