package ar.edu.itba.certiflow.usecases.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class PlanCorrectiveAction {

    private final FindingRepository findingRepository;
    private final Clock clock;

    public PlanCorrectiveAction(FindingRepository findingRepository, Clock clock) {
        this.findingRepository = findingRepository;
        this.clock = clock;
    }

    public CorrectiveActionId execute(FindingId findingId, String description, Person responsible, LocalDate dueDate,
                                    Person plannedBy) {
        Finding finding = findingRepository.findById(findingId).orElseThrow(() -> new FindingNotFoundException(findingId));
        CorrectiveAction plannedAction = finding.planAction(description, responsible, dueDate, plannedBy, clock.instant(),
                LocalDate.now(clock));
        findingRepository.save(finding);
        return plannedAction.id();
    }
}
