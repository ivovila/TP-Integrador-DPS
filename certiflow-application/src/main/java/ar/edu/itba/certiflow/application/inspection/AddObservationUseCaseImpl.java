package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.usecase.AddObservationCommand;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.AddObservationUseCase;
import java.time.Clock;

public final class AddObservationUseCaseImpl implements AddObservationUseCase {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public AddObservationUseCaseImpl(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public void execute(AddObservationCommand command) {
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        inspectionService.addObservation(inspection, command.criterionCode(), command.text(), command.observedBy(), clock.instant());
        inspectionRepository.save(inspection);
    }
}
