package ar.edu.itba.certiflow.domain.model.asset;

public record AssetType(String name) {

    public AssetType {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El tipo de activo no puede estar vacio");
        }
        name = name.trim().toLowerCase();
    }
}
