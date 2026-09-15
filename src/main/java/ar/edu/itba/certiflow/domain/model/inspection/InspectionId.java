package ar.edu.itba.certiflow.domain.model.inspection;

import java.util.UUID;

import ar.edu.itba.certiflow.domain.model.shared.AggregateId;

public record InspectionId(UUID value) implements AggregateId {

    public static InspectionId generate() {
        return new InspectionId(UUID.randomUUID());
    }
}
