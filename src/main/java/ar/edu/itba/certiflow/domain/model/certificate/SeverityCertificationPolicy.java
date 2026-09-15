package ar.edu.itba.certiflow.domain.model.certificate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;

public record SeverityCertificationPolicy(Severity blockingSeverity) implements CertificationPolicy {

    @Override
    public boolean allows(InspectionEvaluation evaluation, List<Finding> findings) {
        Set<CriterionId> covered = findings.stream()
                .map(Finding::getCriterion)
                .collect(Collectors.toSet());
        boolean everyRejectionHasFinding = covered.containsAll(evaluation.criteriaWith(CriterionOutcome.REJECTED));
        boolean noBlockingFindingOpen = findings.stream()
                .filter(Finding::isOpen)
                .noneMatch(finding -> finding.getSeverity().isAtLeast(blockingSeverity));
        return everyRejectionHasFinding && noBlockingFindingOpen;
    }
}
