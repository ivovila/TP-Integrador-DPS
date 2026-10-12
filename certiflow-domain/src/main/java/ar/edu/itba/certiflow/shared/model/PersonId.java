package ar.edu.itba.certiflow.shared.model;

public record PersonId(String value) {

    public PersonId {
        if (value.isBlank()) {
            throw new RequiredValueMissingException("A person id must not be blank");
        }
    }
}
