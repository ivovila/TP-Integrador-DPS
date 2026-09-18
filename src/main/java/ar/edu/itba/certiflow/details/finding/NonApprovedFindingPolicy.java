package ar.edu.itba.certiflow.details.finding;

import ar.edu.itba.certiflow.models.evaluation.CriterionEvaluationResult;
import ar.edu.itba.certiflow.models.evaluation.Outcome;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingPolicy;
import java.util.Optional;
import java.util.UUID;

/** Creates a finding for every evaluation that was not approved. */
public class NonApprovedFindingPolicy implements FindingPolicy {
    @Override
    public Optional<Finding> create(CriterionEvaluationResult<?> evaluation) {
        if (evaluation.outcome() == Outcome.APPROVED) {
            return Optional.empty();
        }
        return Optional.of(new Finding(UUID.randomUUID(), evaluation));
    }
}
