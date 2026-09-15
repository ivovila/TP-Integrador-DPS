package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record FindingClosed(FindingId findingId, LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public FindingId aggregateId() {
        return findingId;
    }
}
