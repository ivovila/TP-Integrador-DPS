package ar.edu.itba.certiflow.infrastructure.shared.persistence;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;

public record PersonColumns(String id, String name) {

    public PersonColumns(Person person) {
        this(person.id().value(), person.name());
    }

    public Person toDomain() {
        return new Person(new PersonId(id), name);
    }
}
