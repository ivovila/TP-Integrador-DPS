package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.List;

public record GivenAnswer<A extends Answer>(A value, Stamp answered) implements Answering<A> {

    /** Una respuesta sin la evidencia que el criterio exige todavía no tiene resultado. */
    @Override
    public Outcome outcomeFor(Criterion<A> criterion, List<Evidence> attachedEvidence) {
        return criterion.evidenceRequirementsMetBy(attachedEvidence) ? criterion.outcomeOf(value) : Outcome.PENDING;
    }

    @Override
    public <T> T describedBy(AnsweringDescriber<A, T> describer) {
        return describer.answered(this);
    }

    public Person answeredBy() {
        return answered.author();
    }

    public Instant answeredAt() {
        return answered.instant();
    }
}
