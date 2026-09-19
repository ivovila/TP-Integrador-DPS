package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;

public final class RectifyInspection {

    private final InspectionRepository inspectionRepository;
    private final Clock clock;

    public RectifyInspection(InspectionRepository inspectionRepository, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.clock = clock;
    }

    public <A extends Answer> void execute(Inspection inspection, Criterion<A> criterion, A answer, String reason,
                                           Person rectifiedBy) {
        inspection.rectify(criterion, answer, reason, rectifiedBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
