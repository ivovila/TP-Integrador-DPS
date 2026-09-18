package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.util.Objects;

public final class AttachEvidence {

    private final InspectionRepository inspections;
    private final Clock clock;

    public AttachEvidence(InspectionRepository inspections, Clock clock) {
        this.inspections = Objects.requireNonNull(inspections, "inspections");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public void execute(Inspection inspection, Criterion<?> criterion, Evidence evidence, Person by) {
        inspection.attachEvidence(criterion, evidence, by, clock.instant());
        inspections.save(inspection);
    }
}
