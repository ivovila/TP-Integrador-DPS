package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Characteristics characteristics) {

    public Asset {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset needs a name");
        }
    }
}
