package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.FindingService;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.usecases.finding.PlanCorrectiveAction;
import java.time.Clock;
import java.time.LocalDate;

public final class PlanCorrectiveActionInteractor implements PlanCorrectiveAction {

    private final FindingRepository findingRepository;
    private final FindingService findingService;
    private final Clock clock;

    public PlanCorrectiveActionInteractor(FindingRepository findingRepository, FindingService findingService, Clock clock) {
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
