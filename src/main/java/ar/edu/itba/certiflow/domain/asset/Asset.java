package ar.edu.itba.certiflow.domain.asset;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.util.Map;

public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Map<String, String> characteristics) {

    public Asset {
        characteristics = Map.copyOf(characteristics);
        if (name.isBlank()) {
            throw new IllegalArgumentException("An asset needs a name");
        }
    }
}
