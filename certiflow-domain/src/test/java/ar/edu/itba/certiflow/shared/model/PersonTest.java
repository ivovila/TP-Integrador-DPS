package ar.edu.itba.certiflow.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class PersonTest {

    @Test
    void namesakesWithDifferentIdsAreDifferentPeople() {
        assertNotEquals(new Person(new PersonId("EMP-100"), "Juan Perez"), new Person(new PersonId("EMP-200"), "Juan Perez"));
    }

    @Test
    void samePersonIsRecognisedByIdEvenIfTheNameIsWrittenDifferently() {
        Person person = new Person(new PersonId("EMP-100"), "Juan Perez");
        Person sameWithFullName = new Person(new PersonId("EMP-100"), "Juan Carlos Perez");

        assertEquals(person, sameWithFullName);
        assertEquals(person.hashCode(), sameWithFullName.hashCode());
    }
}
