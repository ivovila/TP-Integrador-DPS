package ar.edu.itba.certiflow.domain.asset;

import java.util.Objects;

/** Identificador de negocio: la placa o etiqueta con la que la entidad reconoce al activo. */
public record AssetCode(String value) {

    public AssetCode {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("An asset code must not be blank");
        }
    }
}
