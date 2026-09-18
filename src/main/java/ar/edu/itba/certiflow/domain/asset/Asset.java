package ar.edu.itba.certiflow.domain.asset;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.util.Map;
import java.util.Objects;

/**
 * En esta entrega el catálogo solo exige alta y consulta, por eso el activo es inmutable.
 * Su identidad de negocio es el código; el catálogo garantiza que no haya dos activos con el mismo.
 */
public record Asset(AssetCode code, String name, AssetType type, Location location, Person responsible,
                    Map<String, String> characteristics) {

    public Asset {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(location, "location");
        Objects.requireNonNull(responsible, "responsible");
        characteristics = Map.copyOf(characteristics);
        if (name.isBlank()) {
            throw new IllegalArgumentException("An asset needs a name");
        }
    }
}
