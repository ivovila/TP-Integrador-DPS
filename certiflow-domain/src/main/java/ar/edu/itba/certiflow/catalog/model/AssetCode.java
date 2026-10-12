package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record AssetCode(String value) {

    public AssetCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("An asset code must not be blank");
        }
    }
}
