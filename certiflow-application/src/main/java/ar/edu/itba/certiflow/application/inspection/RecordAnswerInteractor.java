package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionService;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.inspection.RecordAnswer;
import java.time.Clock;

public final class RecordAnswerInteractor implements RecordAnswer {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public RecordAnswerInteractor(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
        this.inspectionRepository = inspectionRepository;
        this.inspectionService = inspectionService;
        this.clock = clock;
    }

    @Override
    public <A extends Answer> void execute(InspectionId inspectionId, Criterion<A> criterion, A answer, Person answeredBy) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        inspectionService.recordAnswer(inspection, criterion, answer, answeredBy, clock.instant());
        inspectionRepository.save(inspection);
    }
}
