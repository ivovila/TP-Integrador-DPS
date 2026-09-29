package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.shared.RequiredValueMissingException;

public record Characteristic(String name, String value) {

    public Characteristic {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("A characteristic needs a name");
        }
        if (value.isBlank()) {
            throw new RequiredValueMissingException("Characteristic '" + name + "' needs a value");
        }
    }
}
