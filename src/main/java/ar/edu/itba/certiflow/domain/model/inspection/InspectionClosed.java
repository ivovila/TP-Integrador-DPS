package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record InspectionClosed(InspectionId inspectionId, LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public InspectionId aggregateId() {
        return inspectionId;
    }
}
