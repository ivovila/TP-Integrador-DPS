package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerCommand;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import java.time.Clock;

public final class RecordAnswerUseCaseImpl implements RecordAnswerUseCase {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public RecordAnswerUseCaseImpl(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public void execute(RecordAnswerCommand command) {
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        inspectionService.recordAnswer(inspection, command.criterionCode(), command.answer(), command.answeredBy(), clock.instant());
        inspectionRepository.save(inspection);
    }
}
