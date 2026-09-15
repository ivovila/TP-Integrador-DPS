package ar.edu.itba.certiflow.domain.model.finding;

import java.util.UUID;

public record CorrectiveActionId(UUID value) {

    public static CorrectiveActionId generate() {
        return new CorrectiveActionId(UUID.randomUUID());
    }
}
