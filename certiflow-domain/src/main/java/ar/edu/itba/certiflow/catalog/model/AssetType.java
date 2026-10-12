package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record AssetType(String name) {

    public AssetType {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset type needs a name");
        }
    }
}
