package ar.edu.itba.certiflow.web.support;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.PersonId;

public record PersonPayload(String id, String name) {

    public PersonPayload {
        RequestFields.require(id, "id");
        RequestFields.require(name, "name");
    }

    public static PersonPayload of(Person person) {
        return new PersonPayload(person.id().value(), person.name());
    }

    public Person toDomain() {
        return new Person(new PersonId(id), name);
    }
}
