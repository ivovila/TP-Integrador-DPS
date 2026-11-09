package ar.edu.itba.certiflow.assistance.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record ReviewId(String value) {

    public ReviewId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A review id must not be blank");
        }
    }
}
