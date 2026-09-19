package ar.edu.itba.certiflow.models.asset;

public record AssetCode(String value) {

    public AssetCode {
        if (value.isBlank()) {
            throw new IllegalArgumentException("An asset code must not be blank");
        }
    }
}
