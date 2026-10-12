package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.InspectionRectified;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import java.time.Clock;
import java.util.List;

public final class RectifyInspectionUseCaseImpl implements RectifyInspectionUseCase {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final InspectionService inspectionService;
    private final FindingService findingService;
    private final Clock clock;

    public RectifyInspectionUseCaseImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                       InspectionService inspectionService, FindingService findingService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.inspectionService = inspectionService;
        this.findingService = findingService;
        this.clock = clock;
    }

    @Override
    public <A extends Answer> List<FindingId> execute(InspectionId inspectionId, Criterion<A> criterion, A answer,
                                                      String reason, Person rectifiedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        InspectionRectified rectification = inspectionService.rectify(inspection, criterion, answer, reason, rectifiedBy,
                clock.instant());
        List<Finding> raisedFindings = findingService.raiseFrom(rectification,
                findingRepository.findByAsset(inspection.assetCode()), findingRepository::nextId);
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
