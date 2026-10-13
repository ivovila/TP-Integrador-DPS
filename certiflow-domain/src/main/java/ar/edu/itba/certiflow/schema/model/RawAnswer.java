package ar.edu.itba.certiflow.schema.model;

import java.util.Objects;
import java.util.Optional;

/** El valor tal como llegó, antes de que una regla lo interprete. Cada regla pide el tipo que sabe leer. */
public record RawAnswer(Object value) {

    public RawAnswer {
        Objects.requireNonNull(value, "value");
    }

    public <T> Optional<T> as(Class<T> type) {
        return Optional.of(value).filter(type::isInstance).map(type::cast);
    }
}
