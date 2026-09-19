package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;

public final class AddObservation {

    private final InspectionRepository inspectionRepository;
    private final Clock clock;

    public AddObservation(InspectionRepository inspectionRepository, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.clock = clock;
    }

    public void execute(Inspection inspection, Criterion<?> criterion, String observationText, Person observedBy) {
        inspection.addObservation(criterion, observationText, observedBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
