package ar.edu.itba.certiflow.domain.model.inspection;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record InspectionStarted(InspectionId inspectionId, int schemaVersion, LocalDateTime occurredAt)
        implements DomainEvent {

    @Override
    public InspectionId aggregateId() {
        return inspectionId;
    }
}
