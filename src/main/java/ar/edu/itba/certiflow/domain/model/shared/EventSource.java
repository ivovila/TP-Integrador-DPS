package ar.edu.itba.certiflow.domain.model.shared;

import java.util.List;

public interface EventSource {

    List<DomainEvent> pullEvents();
}
