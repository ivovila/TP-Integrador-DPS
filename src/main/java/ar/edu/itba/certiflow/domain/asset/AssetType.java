package ar.edu.itba.certiflow.domain.asset;

public record AssetType(String name) {

    public AssetType {
        if (name.isBlank()) {
            throw new IllegalArgumentException("An asset type needs a name");
        }
    }
}
