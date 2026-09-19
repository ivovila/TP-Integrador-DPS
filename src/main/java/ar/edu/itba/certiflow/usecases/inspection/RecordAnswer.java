package ar.edu.itba.certiflow.usecases.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import java.time.Clock;

public final class RecordAnswer {

    private final InspectionRepository inspectionRepository;
    private final Clock clock;

    public RecordAnswer(InspectionRepository inspectionRepository, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.clock = clock;
    }

    public <A extends Answer> void execute(Inspection inspection, Criterion<A> criterion, A answer, Person answeredBy) {
        inspection.recordAnswer(criterion, answer, answeredBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
