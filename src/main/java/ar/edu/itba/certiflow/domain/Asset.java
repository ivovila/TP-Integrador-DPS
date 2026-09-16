package ar.edu.itba.certiflow.domain;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record Asset(
        UUID id,
        String name,
        String type,
        String location,
        String responsible,
        Map<String, String> characteristics) {
    public Asset {
        Objects.requireNonNull(id);
        name = Checks.text(name, "name");
        type = Checks.text(type, "type");
        location = Checks.text(location, "location");
        responsible = Checks.text(responsible, "responsible");
        characteristics = Map.copyOf(characteristics);
        characteristics.forEach(
                (key, value) -> {
                    Checks.text(key, "characteristic");
                    Checks.text(value, "value");
                });
    }
}
