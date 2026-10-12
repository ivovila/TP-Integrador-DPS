package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
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
    public void execute(InspectionId inspectionId, Criterion<?> criterion, Evidence evidence, Person attachedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        inspectionService.attachEvidence(inspection, criterion, evidence, attachedBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
