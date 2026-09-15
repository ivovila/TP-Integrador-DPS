package ar.edu.itba.certiflow.domain.model.shared;

import java.time.LocalDateTime;

public interface DomainEvent {

    String aggregateId();

    LocalDateTime occurredAt();
}
