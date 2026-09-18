package ar.edu.itba.certiflow.domain.asset;


public record Location(String description) {

    public Location {
        if (description.isBlank()) {
            throw new IllegalArgumentException("A location needs a description");
        }
    }
}
