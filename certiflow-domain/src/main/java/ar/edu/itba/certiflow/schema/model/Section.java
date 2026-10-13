package ar.edu.itba.certiflow.schema.model;

import ar.edu.itba.certiflow.catalog.model.NoSubsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.schema.model.exceptions.InvalidSchemaException;
import java.util.List;

public record Section(SectionTitle title, List<Criterion<?>> criteria, SubsystemAssociation subsystem) {


    public Section(SectionTitle title, List<Criterion<?>> criteria) {
        this(title, criteria, new NoSubsystem());
    }

    public Section {
        criteria = List.copyOf(criteria);
        if (criteria.isEmpty()) {
            throw new InvalidSchemaException("Section '" + title.value() + "' needs at least one criterion");
        }
    }
}
