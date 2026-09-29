package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;


public record Location(String description) {

    public Location {
        if (description.isBlank()) {
            throw new RequiredValueMissingException("A location needs a description");
        }
    }
}
