package ar.edu.itba.certiflow.domain.shared;

import java.util.UUID;

// Identifica a una persona: responsable de un activo, inspector, responsable de un hallazgo.
public record PersonId(UUID value) {

    public static PersonId generate() {
        return new PersonId(UUID.randomUUID());
    }
}
