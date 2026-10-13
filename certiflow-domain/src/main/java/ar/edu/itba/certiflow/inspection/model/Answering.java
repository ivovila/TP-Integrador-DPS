package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.Outcome;
import java.util.List;

/** Lo que se sabe de la respuesta de un criterio: todavía ninguna, o la que se dio. */
public interface Answering<A extends Answer> {

    Outcome outcomeFor(Criterion<A> criterion, List<Evidence> attachedEvidence);

    <T> T describedBy(AnsweringDescriber<A, T> describer);
}
