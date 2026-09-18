package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.util.Objects;

public final class RecordAnswer {

    private final InspectionRepository inspections;
    private final Clock clock;

    public RecordAnswer(InspectionRepository inspections, Clock clock) {
        this.inspections = Objects.requireNonNull(inspections, "inspections");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public <A extends Answer> void execute(Inspection inspection, Criterion<A> criterion, A answer, Person by) {
        inspection.recordAnswer(criterion, answer, by, clock.instant());
        inspections.save(inspection);
    }
}
