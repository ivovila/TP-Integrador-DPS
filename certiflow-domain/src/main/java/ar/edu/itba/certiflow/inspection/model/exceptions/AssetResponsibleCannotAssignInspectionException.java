package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.Person;

public class AssetResponsibleCannotAssignInspectionException extends DomainException {

    public AssetResponsibleCannotAssignInspectionException(Person person) {
        super(person.name() + " is responsible for the asset and cannot assign its inspection");
    }
}
