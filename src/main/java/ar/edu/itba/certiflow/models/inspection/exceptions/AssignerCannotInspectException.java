package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import ar.edu.itba.certiflow.models.shared.Person;

public class AssignerCannotInspectException extends DomainException {

    public AssignerCannotInspectException(Person person) {
        super(person.name() + " assigned the inspection and cannot also perform it");
    }
}
