package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;

public final class AttachEvidence {

    private final InspectionRepository inspectionRepository;
    private final Clock clock;

    public AttachEvidence(InspectionRepository inspectionRepository, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.clock = clock;
    }

    public void execute(Inspection inspection, Criterion<?> criterion, Evidence evidence, Person attachedBy) {
        inspection.attachEvidence(criterion, evidence, attachedBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
