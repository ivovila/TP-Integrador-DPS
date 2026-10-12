package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionService;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.RecordAnswerUseCase;
import java.time.Clock;

public final class RecordAnswerUseCaseImpl implements RecordAnswerUseCase {

    private final InspectionRepository inspectionRepository;
    private final InspectionService inspectionService;
    private final Clock clock;

    public RecordAnswerUseCaseImpl(InspectionRepository inspectionRepository, InspectionService inspectionService, Clock clock) {
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
