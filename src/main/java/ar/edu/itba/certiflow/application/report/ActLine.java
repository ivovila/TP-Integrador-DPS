package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

public record ActLine(String criterionCode, String criterionText, Answer answer, Person answeredBy,
                      Instant answeredAt, int evidenceCount, List<String> observations, Outcome outcome) {

    public ActLine {
        observations = List.copyOf(observations);
    }
}
