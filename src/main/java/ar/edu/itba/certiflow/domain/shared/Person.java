package ar.edu.itba.certiflow.domain.shared;

public record Person(String name) {

    public Person {
        if (name.isBlank()) {
            throw new IllegalArgumentException("A person needs a name");
        }
    }
}
