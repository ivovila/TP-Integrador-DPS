package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.util.Objects;

public record Subsystem(SubsystemCode code, String name) {
    public Subsystem {
        Objects.requireNonNull(code);
        if (name == null || name.isBlank()) { throw new RequiredValueMissingException("Subsystem name is required"); }
    }
}
