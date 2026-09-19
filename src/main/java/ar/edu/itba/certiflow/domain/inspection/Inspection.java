package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public interface Inspection extends InspectionView {

    <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person by, Instant at);

    void attachEvidence(Criterion<?> criterion, Evidence evidence, Person by, Instant at);

    void addObservation(Criterion<?> criterion, String text, Person by, Instant at);

    void close(Person by, Instant at);

    <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person by, Instant at);
}
