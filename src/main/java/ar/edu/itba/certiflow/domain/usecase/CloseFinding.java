package ar.edu.itba.certiflow.domain.usecase;

import ar.edu.itba.certiflow.domain.model.finding.Finding;
import ar.edu.itba.certiflow.domain.model.finding.FindingId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.ports.Clock;
import ar.edu.itba.certiflow.domain.ports.EventPublisher;
import ar.edu.itba.certiflow.domain.ports.FindingRepository;

public class CloseFinding {

    private final FindingRepository findings;
    private final Clock clock;
    private final EventPublisher events;

    public CloseFinding(FindingRepository findings, Clock clock, EventPublisher events) {
        this.findings = findings;
        this.clock = clock;
        this.events = events;
    }

    public void execute(FindingId findingId) {
        Finding finding = findings.findById(findingId)
                .orElseThrow(() -> new NotFoundException("el hallazgo", findingId.value()));
        finding.close(clock.now());
        findings.save(finding);
        finding.pullEvents().forEach(events::publish);
    }
}
