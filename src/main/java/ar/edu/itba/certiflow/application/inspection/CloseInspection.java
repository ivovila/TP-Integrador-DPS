package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.application.finding.FindingRepository;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.finding.AuditedFindingGenerator;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class CloseInspection {

    private final InspectionRepository inspections;
    private final FindingRepository findings;
    private final AuditedFindingGenerator findingGenerator;
    private final Clock clock;

    public CloseInspection(InspectionRepository inspections, FindingRepository findings,
                           AuditedFindingGenerator findingGenerator, Clock clock) {
        this.inspections = inspections;
        this.findings = findings;
        this.findingGenerator = findingGenerator;
        this.clock = clock;
    }

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
