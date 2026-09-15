package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDateTime;

import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;

public record FindingRaised(FindingId findingId, InspectionId inspectionId, Severity severity,
                            LocalDateTime occurredAt) implements DomainEvent {

    @Override
    public String aggregateId() {
        return findingId.value().toString();
    }
}
