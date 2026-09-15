package ar.edu.itba.certiflow.domain.ports;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.EventSource;

public interface EventPublisher {

    void publish(DomainEvent event);

    default void publishFrom(EventSource source) {
        source.pullEvents().forEach(this::publish);
    }
}
