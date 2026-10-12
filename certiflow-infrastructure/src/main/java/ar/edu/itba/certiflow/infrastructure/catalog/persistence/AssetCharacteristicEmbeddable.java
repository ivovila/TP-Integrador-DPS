package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AssetCharacteristicEmbeddable {

    @Column(name = "name")
    private String name;
    @Column(name = "characteristic_value")
    private String value;

    protected AssetCharacteristicEmbeddable() {
    }

    AssetCharacteristicEmbeddable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    String name() {
        return name;
    }

    String value() {
        return value;
    }
}
