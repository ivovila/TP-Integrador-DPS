package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.inspection.model.exceptions.DuplicateEvidenceException;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.Outcome;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record CriterionResponse<A extends Answer>(Criterion<A> criterion, Answering<A> answering,
                                                  List<AttachedEvidence> attachments,
                                                  List<Observation> observations) {

    public CriterionResponse {
        attachments = List.copyOf(attachments);
        observations = List.copyOf(observations);
    }

    public CriterionResponse(Criterion<A> criterion) {
        this(criterion, new NotAnswered<>(), List.of(), List.of());
    }

    /** La respuesta dada, si ya se respondió. */
    public Optional<GivenAnswer<A>> given() {
        return answering.describedBy(new AnswerIfGiven<>());
    }

    public <B extends Answer> CriterionResponse<B> answeredWith(Criterion<B> answeredCriterion, B answer, Stamp answered) {
        if (!criterion.equals(answeredCriterion)) {
            throw new CriterionOutOfScopeException(answeredCriterion);
        }
        return new CriterionResponse<>(answeredCriterion, new GivenAnswer<>(answer, answered), attachments,
                observations);
    }

    public CriterionResponse<A> withEvidence(Evidence evidence, Stamp attached) {
        if (evidence().stream().anyMatch(evidence::isSameFileAs)) {
            throw new DuplicateEvidenceException(criterion.code(), evidence.reference());
        }
        return new CriterionResponse<>(criterion, answering,
                Stream.concat(attachments.stream(), Stream.of(new AttachedEvidence(evidence, attached))).toList(),
                observations);
    }

    public CriterionResponse<A> withObservation(Observation addedObservation) {
        return new CriterionResponse<>(criterion, answering, attachments,
                Stream.concat(observations.stream(), Stream.of(addedObservation)).toList());
    }

    private List<Evidence> evidence() {
        return attachments.stream().map(AttachedEvidence::evidence).toList();
    }

    public Outcome outcome() {
        return answering.outcomeFor(criterion, evidence());
    }

    public boolean isFor(Criterion<?> candidate) {
        return criterion.equals(candidate);
    }
}
