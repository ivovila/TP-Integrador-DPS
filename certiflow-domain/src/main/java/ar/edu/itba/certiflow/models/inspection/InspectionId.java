package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record InspectionId(String value) {

    public InspectionId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("An inspection id must not be blank");
        }
    }
}
