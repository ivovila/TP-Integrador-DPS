package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.evaluation.CriterionEvaluationResult;
import java.util.UUID;

public record Finding(UUID id, CriterionEvaluationResult<?> evaluation, CorrectiveActionPolicy correctiveActionPolicy) {
}
