package ar.edu.itba.certiflow.domain.model.shared;

import java.util.UUID;

public record PersonId(UUID value) {

    public static PersonId generate() {
        return new PersonId(UUID.randomUUID());
    }
}
