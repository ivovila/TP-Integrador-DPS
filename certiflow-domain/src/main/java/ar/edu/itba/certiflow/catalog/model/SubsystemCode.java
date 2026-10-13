package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record SubsystemCode(String value) {
    public SubsystemCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("Subsystem code is required");
        }
    }
}
