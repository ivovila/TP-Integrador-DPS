package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record FindingId(String value) {

    public FindingId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A finding id must not be blank");
        }
    }
}
