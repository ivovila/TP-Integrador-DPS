package ar.edu.itba.certiflow.domain.model.report;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.finding.Severity;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record FindingsSummary(InspectionId inspection, Map<Severity, Long> openBySeverity, long closed,
                              List<FindingLine> findings) {

    public FindingsSummary {
        openBySeverity = Map.copyOf(openBySeverity);
        findings = List.copyOf(findings);
    }

    public static FindingsSummary of(InspectionId inspection, List<Finding> inspectionFindings, LocalDate today) {
        if (inspectionFindings.stream().anyMatch(finding -> !finding.getInspection().equals(inspection))) {
            throw new IllegalArgumentException("Los hallazgos recibidos no son de esta inspeccion");
        }
        Map<Severity, Long> openBySeverity = inspectionFindings.stream()
                .filter(Finding::isOpen)
                .collect(Collectors.groupingBy(Finding::getSeverity, () -> new EnumMap<>(Severity.class),
                        Collectors.counting()));
        long closed = inspectionFindings.stream().filter(finding -> !finding.isOpen()).count();
        List<FindingLine> lines = inspectionFindings.stream()
                .map(finding -> FindingLine.of(finding, today))
                .toList();
        return new FindingsSummary(inspection, openBySeverity, closed, lines);
    }

    public long open() {
        return openBySeverity.values().stream().mapToLong(Long::longValue).sum();
    }

    public record FindingLine(FindingId finding, CriterionId criterion, Severity severity, String description,
                              PersonId responsible, boolean open, int actions, long overdueActions) {

        static FindingLine of(Finding finding, LocalDate today) {
            long overdue = finding.getActions().stream().filter(action -> action.isOverdue(today)).count();
            return new FindingLine(finding.getId(), finding.getCriterion(), finding.getSeverity(),
                    finding.getDescription(), finding.getResponsible(), finding.isOpen(),
                    finding.getActions().size(), overdue);
        }
    }
}
