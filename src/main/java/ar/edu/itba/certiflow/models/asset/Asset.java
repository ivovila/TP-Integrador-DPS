package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;
import java.util.Map;

public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Map<String, String> characteristics) {

    public Asset {
        characteristics = Map.copyOf(characteristics);
        if (name.isBlank()) {
            throw new RequiredValueMissingException("An asset needs a name");
        }
    }
}
