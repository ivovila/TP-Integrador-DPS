package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.FindingService;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionClosed;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.CloseInspection;
import java.time.Clock;
import java.util.List;

public final class CloseInspectionInteractor implements CloseInspection {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final InspectionService inspectionService;
    private final FindingService findingService;
    private final Clock clock;

    public CloseInspectionInteractor(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                     InspectionService inspectionService, FindingService findingService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.inspectionService = inspectionService;
        this.findingService = findingService;
        this.clock = clock;
    }

    @Override
    public List<FindingId> execute(InspectionId inspectionId, Person closedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        InspectionClosed closure = inspectionService.close(inspection, closedBy, clock.instant());
        List<Finding> raisedFindings = findingService.raiseFrom(closure, findingRepository::nextId);
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
