package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;

public final class RecordAnswerUseCaseImpl implements RecordAnswerUseCase {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final AssetLock assetLock;
    private final Clock clock;

    public RecordAnswerUseCaseImpl(InspectionRepository inspectionRepository, InspectionService inspectionService,
                                   AssetLock assetLock, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.assetLock = assetLock;
        this.clock = clock;
    }

    @Override
    public void execute(RecordAnswerCommand command) {
        AssetCode assetCode = inspectionRepository.assetCodeOf(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        assetLock.lock(assetCode);
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        inspectionService.recordAnswer(inspection, command.criterionCode(), command.answer(),
                new Stamp(command.answeredBy(), clock.instant()));
        inspectionRepository.save(inspection);
    }
}
