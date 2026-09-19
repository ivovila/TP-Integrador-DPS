package ar.edu.itba.certiflow.models.asset;


public record Location(String description) {

    public Location {
        if (description.isBlank()) {
            throw new IllegalArgumentException("A location needs a description");
        }
    }
}
