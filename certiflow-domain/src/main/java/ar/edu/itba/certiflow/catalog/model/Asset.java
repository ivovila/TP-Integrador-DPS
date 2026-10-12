package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;

public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Characteristics characteristics) {

    public Asset {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset needs a name");
        }
    }
}
