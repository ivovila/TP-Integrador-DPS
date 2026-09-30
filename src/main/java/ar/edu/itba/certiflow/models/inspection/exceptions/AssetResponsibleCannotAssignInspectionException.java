package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import ar.edu.itba.certiflow.models.shared.Person;

public class AssetResponsibleCannotAssignInspectionException extends DomainException {

    public AssetResponsibleCannotAssignInspectionException(Person person) {
        super(person.name() + " is responsible for the asset and cannot assign its inspection");
    }
}
