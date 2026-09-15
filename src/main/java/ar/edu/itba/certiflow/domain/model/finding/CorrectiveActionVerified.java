package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record CorrectiveActionVerified(FindingId findingId, CorrectiveActionId actionId, PersonId verifier,
                                       LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return findingId.value().toString();
    }
}
