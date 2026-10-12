package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.FindingService;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.InspectionRectified;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.RectifyInspection;
import java.time.Clock;
import java.util.List;

public final class RectifyInspectionInteractor implements RectifyInspection {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final InspectionService inspectionService;
    private final FindingService findingService;
    private final Clock clock;

    public RectifyInspectionInteractor(InspectionRepository inspectionRepository, FindingRepository findingRepository,
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
