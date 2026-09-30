package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.finding.AuditedFindingFactory;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;
import java.util.List;

public final class CloseInspection {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final AuditedFindingFactory findingFactory;
    private final Clock clock;

    public CloseInspection(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                           AuditedFindingFactory findingFactory, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.findingFactory = findingFactory;
        this.clock = clock;
    }

    public List<FindingId> execute(InspectionId inspectionId, Person closedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        List<Finding> raisedFindings = findingFactory.create(inspection.close(closedBy, clock.instant()), findingRepository::nextId);
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
