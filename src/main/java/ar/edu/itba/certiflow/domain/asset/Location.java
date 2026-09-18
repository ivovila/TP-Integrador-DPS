package ar.edu.itba.certiflow.domain.asset;

import java.util.Objects;

public record Location(String description) {

    public Location {
        Objects.requireNonNull(description, "description");
        if (description.isBlank()) {
            throw new IllegalArgumentException("A location needs a description");
        }
    }
}
