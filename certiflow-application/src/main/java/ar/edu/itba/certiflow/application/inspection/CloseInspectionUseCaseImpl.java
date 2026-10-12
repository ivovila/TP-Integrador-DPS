package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionCommand;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionClosed;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.CloseInspectionUseCase;
import java.time.Clock;
import java.util.List;

public final class CloseInspectionUseCaseImpl implements CloseInspectionUseCase {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final InspectionService inspectionService;
    private final FindingService findingService;
    private final Clock clock;

    public CloseInspectionUseCaseImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                     InspectionService inspectionService, FindingService findingService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.inspectionService = inspectionService;
        this.findingService = findingService;
        this.clock = clock;
    }

    @Override
    public List<FindingId> execute(CloseInspectionCommand command) {
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        InspectionClosed closure = inspectionService.close(inspection, command.closedBy(), clock.instant());
        List<Finding> raisedFindings = findingService.raiseFrom(closure, findingRepository::nextId);
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
