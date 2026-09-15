package ar.edu.itba.certiflow.domain.model.shared;

import java.util.ArrayList;
import java.util.List;

public final class PendingEvents {

    private final List<DomainEvent> events = new ArrayList<>();

    public void add(DomainEvent event) {
        events.add(event);
    }

    public List<DomainEvent> pull() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }
}
