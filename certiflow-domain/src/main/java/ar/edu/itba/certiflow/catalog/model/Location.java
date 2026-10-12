package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;


public record Location(String description) {

    public Location {
        if (description.isBlank()) {
            throw new RequiredValueMissingException("A location needs a description");
        }
    }
}
