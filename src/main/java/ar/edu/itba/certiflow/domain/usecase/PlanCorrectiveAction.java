package ar.edu.itba.certiflow.domain.usecase;

import java.time.LocalDate;

import ar.edu.itba.certiflow.domain.model.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.model.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class PlanCorrectiveAction {

    private final FindingRepository findings;
    private final Clock clock;
    private final EventPublisher events;

    public PlanCorrectiveAction(FindingRepository findings, Clock clock, EventPublisher events) {
        this.findings = findings;
        this.clock = clock;
        this.events = events;
    }

    public CorrectiveAction execute(FindingId findingId, String description, PersonId assignee, LocalDate dueDate) {
        Finding finding = findings.findById(findingId)
                .orElseThrow(() -> new NotFoundException("el hallazgo", findingId.value()));
        CorrectiveAction action = finding.planAction(CorrectiveActionId.generate(), description, assignee,
                dueDate, clock.now());
        findings.save(finding);
        finding.pullEvents().forEach(events::publish);
        return action;
    }
}
