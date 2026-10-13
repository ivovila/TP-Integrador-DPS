package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionRectified;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionCommand;
import ar.edu.itba.certiflow.inspection.usecase.RectifyInspectionUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;
import java.util.List;

public final class RectifyInspectionUseCaseImpl implements RectifyInspectionUseCase {

    private final InspectionRepository inspectionRepository;
    private final FindingRepository findingRepository;
    private final InspectionService inspectionService;
    private final FindingService findingService;
    private final AssetLock assetLock;
    private final Clock clock;

    public RectifyInspectionUseCaseImpl(InspectionRepository inspectionRepository, FindingRepository findingRepository,
                                       InspectionService inspectionService, FindingService findingService,
                                       AssetLock assetLock, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.findingRepository = findingRepository;
        this.inspectionService = inspectionService;
        this.findingService = findingService;
        this.assetLock = assetLock;
        this.clock = clock;
    }

    @Override
    public List<FindingId> execute(RectifyInspectionCommand command) {
        AssetCode assetCode = inspectionRepository.assetCodeOf(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        assetLock.lock(assetCode);
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        InspectionRectified rectification = inspectionService.rectify(inspection, command.criterionCode(), command.answer(),
                command.reason(), new Stamp(command.rectifiedBy(), clock.instant()));
        List<Finding> raisedFindings = findingService.raiseFrom(rectification,
                findingRepository.findByAsset(inspection.assetCode()), findingRepository::nextId);
        inspectionRepository.save(inspection);
        findingRepository.saveAll(raisedFindings);
        return raisedFindings.stream().map(Finding::id).toList();
    }
}
