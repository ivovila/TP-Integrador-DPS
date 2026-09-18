package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import java.util.List;

public record ActLine(String criterionCode, String criterionText, Answer answer, int evidenceCount,
                      List<String> observations, Outcome outcome) {

    public ActLine {
        observations = List.copyOf(observations);
    }
}
