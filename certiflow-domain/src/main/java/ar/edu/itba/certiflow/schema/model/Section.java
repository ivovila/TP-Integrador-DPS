package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;
import java.util.Optional;

public record Section(String title, List<Criterion<?>> criteria, Optional<SubsystemCode> subsystemCode) {

    public Section(String title, List<Criterion<?>> criteria) {
        this(title, criteria, Optional.empty());
    }

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
