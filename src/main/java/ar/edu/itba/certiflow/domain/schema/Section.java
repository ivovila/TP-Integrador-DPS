package ar.edu.itba.certiflow.domain.schema;

import java.util.List;
import java.util.Objects;

public record Section(String title, List<Criterion<?>> criteria) {

    public Section {
        Objects.requireNonNull(title, "title");
        criteria = List.copyOf(criteria);
        if (title.isBlank()) {
            throw new InvalidSchemaException("A section needs a title");
        }
        if (criteria.isEmpty()) {
            throw new InvalidSchemaException("Section '" + title + "' needs at least one criterion");
        }
    }
}
