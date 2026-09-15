package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record InspectionRectified(InspectionId inspectionId, PersonId author, String reason,
                                  LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return inspectionId.value().toString();
    }
}
