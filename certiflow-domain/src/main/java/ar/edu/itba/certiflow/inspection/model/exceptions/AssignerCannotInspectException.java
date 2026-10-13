package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.InvalidValueException;
import ar.edu.itba.certiflow.shared.model.Person;

public class AssignerCannotInspectException extends InvalidValueException {

    public AssignerCannotInspectException(Person person) {
        super(person.name() + " assigned the inspection and cannot also perform it");
    }
}
