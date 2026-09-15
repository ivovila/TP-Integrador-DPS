package ar.edu.itba.certiflow.domain.ports;

import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public interface EventPublisher {

    void publish(DomainEvent event);

    default void publishFrom(AggregateRoot aggregate) {
        aggregate.pullEvents().forEach(this::publish);
    }
}
