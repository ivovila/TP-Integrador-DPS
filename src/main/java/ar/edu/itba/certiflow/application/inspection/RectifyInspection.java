package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;

public final class RectifyInspection {

    private final InspectionRepository inspections;
    private final Clock clock;

    public RectifyInspection(InspectionRepository inspections, Clock clock) {
        this.inspections = inspections;
        this.clock = clock;
    }

    public <A extends Answer> void execute(Inspection inspection, Criterion<A> criterion, A answer, String reason,
                                           Person by) {
        inspection.rectify(criterion, answer, reason, by, clock.instant());
        inspections.save(inspection);
    }
}
