package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.finding.AuditedFindingGenerator;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public final class CloseInspection {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final AuditedFindingGenerator findingGenerator;
    private final Clock clock;

    public CloseInspection(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                           AuditedFindingGenerator findingGenerator, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.findingGenerator = findingGenerator;
        this.clock = clock;
    }

    public List<Finding> execute(Inspection inspection, Person closedBy) {
        Instant closedAt = clock.instant();
        inspection.close(closedBy, closedAt);
        List<Finding> raisedFindings = inspection.evaluate().nonConformities().stream()
                .map(nonConformity -> findingGenerator.generate(nonConformity, inspection, closedBy, closedAt))
                .toList();
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings;
    }
}
