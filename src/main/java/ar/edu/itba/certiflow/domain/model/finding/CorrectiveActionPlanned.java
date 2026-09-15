package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDate;
import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record CorrectiveActionPlanned(FindingId findingId, CorrectiveActionId actionId, LocalDate dueDate,
                                      LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public FindingId aggregateId() {
        return findingId;
    }
}
