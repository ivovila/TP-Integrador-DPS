package ar.edu.itba.certiflow.models.shared;

public record Person(String name) {

    public Person {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("A person needs a name");
        }
    }
}
