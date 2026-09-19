package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public interface Inspection extends InspectionView {

    <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person answeredBy, Instant answeredAt);

    void attachEvidence(Criterion<?> criterion, Evidence evidence, Person attachedBy, Instant attachedAt);

    void addObservation(Criterion<?> criterion, String observationText, Person observedBy, Instant observedAt);

    void close(Person closedBy, Instant closedAt);

    <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person rectifiedBy, Instant rectifiedAt);
}
