package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.AttachEvidence;
import java.time.Clock;

public final class AttachEvidenceInteractor implements AttachEvidence {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public AttachEvidenceInteractor(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
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
