package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record InspectionId(String value) {

    public InspectionId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("An inspection id must not be blank");
        }
    }
}
