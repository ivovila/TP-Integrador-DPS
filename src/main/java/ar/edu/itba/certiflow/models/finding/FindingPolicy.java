package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.evaluation.CriterionEvaluationResult;
import java.util.Optional;

@FunctionalInterface
public interface FindingPolicy {
    Optional<Finding> create(CriterionEvaluationResult<?> evaluation);
}
