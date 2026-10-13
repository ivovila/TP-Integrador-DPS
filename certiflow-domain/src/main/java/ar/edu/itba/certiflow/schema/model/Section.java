package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.NoSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;

public record Section(String title, List<Criterion<?>> criteria, SubsystemAssociation subsystem) {


    public Section(String title, List<Criterion<?>> criteria) {
        this(title, criteria, new NoSubsystem());
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
