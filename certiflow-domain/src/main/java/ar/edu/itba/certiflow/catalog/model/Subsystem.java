package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record Subsystem(SubsystemCode code, String name) {
    public Subsystem {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("Subsystem name is required");
        }
    }
}
