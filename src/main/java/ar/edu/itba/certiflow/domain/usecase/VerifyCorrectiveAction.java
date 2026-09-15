package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class VerifyCorrectiveAction {

    private final FindingRepository findings;
    private final Clock clock;
    private final EventPublisher events;

    public VerifyCorrectiveAction(FindingRepository findings, Clock clock, EventPublisher events) {
        this.findings = findings;
        this.clock = clock;
        this.events = events;
    }

    public void execute(FindingId findingId, CorrectiveActionId actionId, PersonId verifier) {
        Finding finding = findings.getById(findingId);
        finding.verifyAction(actionId, verifier, clock.now());
        findings.save(finding);
        events.publishFrom(finding);
    }
}
