package ar.edu.itba.certiflow.domain.model.audit;

import java.util.ArrayList;
import java.util.List;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public class AuditLog {

    private final List<DomainEvent> events = new ArrayList<>();

    public void record(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> history(AggregateId aggregateId) {
        return events.stream()
                .filter(event -> event.aggregateId().equals(aggregateId))
                .toList();
    }
}
