package ar.edu.itba.certiflow.domain;

import java.util.Objects;

public final class Checks {
    private Checks() {}

    public static String text(String value, String name) {
        Objects.requireNonNull(value, name);
        require(!value.isBlank(), name + " must not be blank");
        return value;
    }

    public static void require(boolean condition, String message) {
        if (!condition) throw new DomainException(message);
    }
}
