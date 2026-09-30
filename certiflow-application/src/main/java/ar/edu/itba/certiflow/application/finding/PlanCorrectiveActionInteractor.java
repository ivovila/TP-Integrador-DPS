package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.finding.CorrectiveAction;
import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.FindingRepository;
import ar.edu.itba.certiflow.usecases.finding.PlanCorrectiveAction;
import java.time.Clock;
import java.time.LocalDate;

public final class PlanCorrectiveActionInteractor implements PlanCorrectiveAction {

    private final FindingRepository findingRepository;
    private final AuditLog<Finding> findingLog;
    private final Clock clock;

    public PlanCorrectiveActionInteractor(FindingRepository findingRepository, AuditLog<Finding> findingLog, Clock clock) {
        this.findingRepository = findingRepository;
        this.findingLog = findingLog;
        this.clock = clock;
    }

    @Override
    public CorrectiveActionId execute(FindingId findingId, String description, Person responsible, LocalDate dueDate,
                                              Person plannedBy) {
        Finding finding = findingRepository.findById(findingId).orElseThrow(() -> new FindingNotFoundException(findingId));
        CorrectiveAction plannedAction = finding.planAction(description, responsible, dueDate, plannedBy, clock.instant(),
                LocalDate.now(clock));
        findingRepository.save(finding);
        findingLog.recordAll(finding, finding.pullAuditEntries());
        return plannedAction.id();
    }
}
