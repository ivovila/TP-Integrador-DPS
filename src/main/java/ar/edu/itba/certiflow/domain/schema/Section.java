package ar.edu.itba.certiflow.domain.schema;

import ar.edu.itba.certiflow.domain.schema.exceptions.InvalidSchemaException;

import java.util.List;

public record Section(String title, List<Criterion<?>> criteria) {

    public Section {
        criteria = List.copyOf(criteria);
        if (title.isBlank()) {
            throw new InvalidSchemaException("A section needs a title");
        }
        if (criteria.isEmpty()) {
            throw new InvalidSchemaException("Section '" + title + "' needs at least one criterion");
        }
    }
}
