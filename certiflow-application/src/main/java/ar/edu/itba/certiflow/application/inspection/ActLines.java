package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.AnsweringDescriber;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.GivenAnswer;
import ar.edu.itba.certiflow.inspection.model.Observation;
import ar.edu.itba.certiflow.inspection.usecase.ActLine;
import ar.edu.itba.certiflow.schema.model.Answer;
import java.util.List;

/** El acta lista los criterios respondidos: uno sin responder no aporta ninguna línea. */
final class ActLines<A extends Answer> implements AnsweringDescriber<A, List<ActLine>> {

    private final CriterionResponse<A> response;

    ActLines(CriterionResponse<A> response) {
        this.response = response;
    }

    @Override
    public List<ActLine> notAnswered() {
        return List.of();
    }

    @Override
    public List<ActLine> answered(GivenAnswer<A> givenAnswer) {
        return List.of(new ActLine(response.criterion().code(), response.criterion().text(), givenAnswer.value(),
                givenAnswer.answered(), response.attachments().size(),
                response.observations().stream().map(Observation::text).toList(), response.outcome()));
    }
}
