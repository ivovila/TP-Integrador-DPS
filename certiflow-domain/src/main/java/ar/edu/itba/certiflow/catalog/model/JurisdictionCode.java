package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record JurisdictionCode(String value) {
    public JurisdictionCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A jurisdiction code is required");
        }
    }
}
