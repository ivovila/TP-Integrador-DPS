package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;

public record NotAnswered<A extends Answer>() implements Answering<A> {

    @Override
    public Outcome outcomeFor(Criterion<A> criterion, List<Evidence> attachedEvidence) {
        return Outcome.PENDING;
    }

    @Override
    public <T> T describedBy(AnsweringDescriber<A, T> describer) {
        return describer.notAnswered();
    }
}
