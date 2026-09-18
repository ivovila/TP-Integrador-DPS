package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.Outcome;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Todo lo registrado para un criterio. Viaja junto a su criterio para que el tipo de la respuesta
 * y el de la regla queden alineados sin conversiones. La respuesta es opcional porque el registro
 * es progresivo: se puede adjuntar evidencia antes de responder.
 */
public record CriterionResponse<A extends Answer>(Criterion<A> criterion, Optional<A> answer,
                                                  List<Evidence> evidence, List<Observation> observations) {

    public CriterionResponse {
        Objects.requireNonNull(criterion, "criterion");
        Objects.requireNonNull(answer, "answer");
        evidence = List.copyOf(evidence);
        observations = List.copyOf(observations);
    }

    /** Estado inicial de todo criterio: sin respuesta, sin evidencia y sin observaciones. */
    public CriterionResponse(Criterion<A> criterion) {
        this(criterion, Optional.empty(), List.of(), List.of());
    }

    public <B extends Answer> CriterionResponse<B> answeredWith(Criterion<B> answeredCriterion, B given) {
        return new CriterionResponse<>(answeredCriterion, Optional.of(given), evidence, observations);
    }

    public CriterionResponse<A> withEvidence(Evidence attached) {
        return new CriterionResponse<>(criterion, answer, Stream.concat(evidence.stream(), Stream.of(attached)).toList(),
                observations);
    }

    public CriterionResponse<A> withObservation(Observation added) {
        return new CriterionResponse<>(criterion, answer, evidence,
                Stream.concat(observations.stream(), Stream.of(added)).toList());
    }

    public Outcome outcome() {
        return answer.map(this::outcomeOfAnswered).orElse(Outcome.PENDING);
    }

    private Outcome outcomeOfAnswered(A given) {
        return criterion.evidenceRequirementsMetBy(evidence) ? criterion.outcomeOf(given) : Outcome.PENDING;
    }

    public boolean isFor(Criterion<?> candidate) {
        return criterion.equals(candidate);
    }
}
