package ar.edu.itba.certiflow.domain.asset;

import java.util.Objects;

public record AssetCode(String value) {

    public AssetCode {
        if (value.isBlank()) {
            throw new IllegalArgumentException("An asset code must not be blank");
        }
    }
}
