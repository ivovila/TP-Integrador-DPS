package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionCommand;
import ar.edu.itba.certiflow.inspection.model.CorrectiveAction;
import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.Finding;
import ar.edu.itba.certiflow.inspection.model.FindingService;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.FindingRepository;
import ar.edu.itba.certiflow.inspection.usecase.PlanCorrectiveActionUseCase;
import java.time.Clock;
import java.time.Instant;
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
    public CorrectiveActionId execute(PlanCorrectiveActionCommand command) {
        Finding finding = findingRepository.findById(command.findingId())
                .orElseThrow(() -> new FindingNotFoundException(command.findingId()));
        Instant plannedAt = clock.instant();
        CorrectiveAction plannedAction = findingService.planAction(finding, command.description(), command.responsible(),
                command.dueDate(), command.plannedBy(), plannedAt, LocalDate.ofInstant(plannedAt, clock.getZone()));
        findingRepository.save(finding);
        return plannedAction.id();
    }
}
