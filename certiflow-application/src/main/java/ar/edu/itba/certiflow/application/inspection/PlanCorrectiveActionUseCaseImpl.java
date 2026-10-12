package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class PlanCorrectiveActionUseCaseImpl implements PlanCorrectiveActionUseCase {

    private final FindingRepository findingRepository;
    private final FindingService findingService;
    private final Clock clock;

    public PlanCorrectiveActionUseCaseImpl(FindingRepository findingRepository, FindingService findingService, Clock clock) {
        this.findingRepository = findingRepository;
        this.findingService = findingService;
        this.clock = clock;
    }

    @Override
    public CorrectiveActionId execute(FindingId findingId, String description, Person responsible, LocalDate dueDate,
                                              Person plannedBy) {
        Finding finding = findingRepository.findById(findingId).orElseThrow(() -> new FindingNotFoundException(findingId));
        CorrectiveAction plannedAction = findingService.planAction(finding, description, responsible, dueDate, plannedBy,
                clock.instant(), LocalDate.now(clock));
        findingRepository.save(finding);
        return plannedAction.id();
    }
}
