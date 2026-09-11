package ar.edu.itba.certiflow.domain.schema;

import java.util.UUID;

public record CriterionId(UUID value) {

    public static CriterionId generate() {
        return new CriterionId(UUID.randomUUID());
    }
}
