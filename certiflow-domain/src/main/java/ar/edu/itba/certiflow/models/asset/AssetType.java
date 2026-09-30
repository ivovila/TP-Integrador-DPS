package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record AssetType(String name) {

    public AssetType {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset type needs a name");
        }
    }
}
