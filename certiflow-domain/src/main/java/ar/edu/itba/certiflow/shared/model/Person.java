package ar.edu.itba.certiflow.shared.model;

public final class Person {

    private final PersonId id;
    private final String name;

    public Person(PersonId id, String name) {
        if (name.isBlank()) {
            throw new RequiredValueMissingException("A person needs a name");
        }
        this.id = id;
        this.name = name;
    }

    public PersonId id() {
        return id;
    }

    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Person person && id.equals(person.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return name + " (" + id.value() + ")";
    }
}
