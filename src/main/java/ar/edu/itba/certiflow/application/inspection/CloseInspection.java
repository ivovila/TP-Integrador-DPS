package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.finding.AuditedFindingGenerator;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class CloseInspection {

    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final AuditedFindingGenerator findingGenerator;
    private final Clock clock;

    public CloseInspection(InspectionRepository inspections, FindingRepository findings,
                           AuditedFindingGenerator findingGenerator, Clock clock) {
        this.inspections = Objects.requireNonNull(inspections, "inspections");
        this.findings = Objects.requireNonNull(findings, "findings");
        this.findingGenerator = Objects.requireNonNull(findingGenerator, "findingGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Cerrar sella el acta y levanta un hallazgo por cada criterio observado o rechazado. */
    public List<Finding> execute(Inspection inspection, Person closedBy) {
        Instant now = clock.instant();
        inspection.close(closedBy, now);
        List<Finding> raised = inspection.evaluate().nonConformities().stream()
                .map(nonConformity -> findingGenerator.generate(nonConformity, inspection, closedBy, now))
                .toList();
        inspections.save(inspection);
        findings.saveAll(raised);
        return raised;
    }
}
