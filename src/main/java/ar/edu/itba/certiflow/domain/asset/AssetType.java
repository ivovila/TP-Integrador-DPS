package ar.edu.itba.certiflow.domain.asset;

import java.util.Objects;

/** El tipo de activo es un dato: determina qué esquema de inspección aplica, no una subclase. */
public record AssetType(String name) {

    public AssetType {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("An asset type needs a name");
        }
    }
}
