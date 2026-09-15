package ar.edu.itba.certiflow.domain.model.inspection;

import java.util.UUID;

public record InspectionId(UUID value) {

    public static InspectionId generate() {
        return new InspectionId(UUID.randomUUID());
    }
}
