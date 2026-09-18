package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.evaluation.CriterionEvaluationResult;
import java.util.Collection;
import java.util.List;

public class FindingGenerator {
    public List<Finding> generateFrom(Collection<CriterionEvaluationResult<?>> evaluations) {
        return evaluations.stream()
                .map(evaluation -> evaluation.criterion().findingPolicy().create(evaluation))
                .flatMap(java.util.Optional::stream)
                .toList();
    }
}
