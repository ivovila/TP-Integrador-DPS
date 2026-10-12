package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record FindingId(String value) {

    public FindingId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A finding id must not be blank");
        }
    }
}
