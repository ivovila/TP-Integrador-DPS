package ar.edu.itba.certiflow.application.finding;

import ar.edu.itba.certiflow.domain.finding.CorrectiveAction;
import ar.edu.itba.certiflow.domain.finding.Finding;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class PlanCorrectiveAction {

    private final FindingRepository findings;
    private final Clock clock;

    public PlanCorrectiveAction(FindingRepository findings, Clock clock) {
        this.findings = Objects.requireNonNull(findings, "findings");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CorrectiveAction execute(Finding finding, String description, Person responsible, LocalDate dueDate,
                                    Person plannedBy) {
        CorrectiveAction action = finding.planAction(description, responsible, dueDate, plannedBy, clock.instant());
        findings.save(finding);
        return action;
    }
}
