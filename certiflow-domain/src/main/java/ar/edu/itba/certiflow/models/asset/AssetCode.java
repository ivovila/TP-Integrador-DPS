package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record AssetCode(String value) {

    public AssetCode {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("An asset code must not be blank");
        }
    }
}
