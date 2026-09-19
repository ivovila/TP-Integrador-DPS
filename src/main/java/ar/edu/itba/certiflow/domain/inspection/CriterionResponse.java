package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record CriterionResponse<A extends Answer>(Criterion<A> criterion, Optional<GivenAnswer<A>> given,
                                                  List<AttachedEvidence> attachments,
                                                  List<Observation> observations) {

    public CriterionResponse {
        attachments = List.copyOf(attachments);
        observations = List.copyOf(observations);
    }

    public CriterionResponse(Criterion<A> criterion) {
        this(criterion, Optional.empty(), List.of(), List.of());
    }

    public <B extends Answer> CriterionResponse<B> answeredWith(Criterion<B> answeredCriterion, B value, Person by,
                                                                Instant at) {
        return new CriterionResponse<>(answeredCriterion, Optional.of(new GivenAnswer<>(value, by, at)), attachments,
                observations);
    }

    public CriterionResponse<A> withEvidence(Evidence evidence, Person by, Instant at) {
        return new CriterionResponse<>(criterion, given,
                Stream.concat(attachments.stream(), Stream.of(new AttachedEvidence(evidence, by, at))).toList(),
                observations);
    }

    public CriterionResponse<A> withObservation(Observation added) {
        return new CriterionResponse<>(criterion, given, attachments,
                Stream.concat(observations.stream(), Stream.of(added)).toList());
    }

    private Optional<A> answer() {
        return given.map(GivenAnswer::value);
    }

    private List<Evidence> evidence() {
        return attachments.stream().map(AttachedEvidence::evidence).toList();
    }

    public Outcome outcome() {
        return answer().map(this::outcomeOfAnswered).orElse(Outcome.PENDING);
    }

    private Outcome outcomeOfAnswered(A value) {
        return criterion.evidenceRequirementsMetBy(evidence()) ? criterion.outcomeOf(value) : Outcome.PENDING;
    }

    public boolean isFor(Criterion<?> candidate) {
        return criterion.equals(candidate);
    }
}
