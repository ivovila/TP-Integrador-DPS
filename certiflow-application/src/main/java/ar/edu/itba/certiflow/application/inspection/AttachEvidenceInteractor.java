package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.AttachEvidence;
import java.time.Clock;

public final class AttachEvidenceInteractor implements AttachEvidence {

    private final InspectionRepository inspectionRepository;
    private final AuditLog<Inspection> inspectionLog;
    private final Clock clock;

    public AttachEvidenceInteractor(InspectionRepository inspectionRepository, AuditLog<Inspection> inspectionLog, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionLog = inspectionLog;
        this.clock = clock;
    }

    @Override
    public void execute(InspectionId inspectionId, Criterion<?> criterion, Evidence evidence, Person attachedBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        inspection.attachEvidence(criterion, evidence, attachedBy, clock.instant());
        inspectionRepository.save(inspection);
        inspectionLog.recordAll(inspection, inspection.pullAuditEntries());
    }
}
