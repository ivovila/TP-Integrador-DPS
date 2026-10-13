package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceCommand;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.AttachEvidenceUseCase;
import java.time.Clock;

public final class AttachEvidenceUseCaseImpl implements AttachEvidenceUseCase {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public AttachEvidenceUseCaseImpl(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public void execute(AttachEvidenceCommand command) {
        Inspection inspection = inspectionRepository.findById(command.inspectionId())
                .orElseThrow(() -> new InspectionNotFoundException(command.inspectionId()));
        inspectionService.attachEvidence(inspection, command.criterionCode(), command.evidence(), new Stamp(command.attachedBy(), clock.instant()));
        inspectionRepository.save(inspection);
    }
}
