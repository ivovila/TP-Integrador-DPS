package ar.edu.itba.certiflow.domain.shared;

import java.util.Objects;


public record Person(String name) {

    public Person {
        if (name.isBlank()) {
            throw new IllegalArgumentException("A person needs a name");
        }
    }
}
