package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import ar.edu.itba.certiflow.shared.model.Person;

public class AssetResponsibleCannotAssignInspectionException extends RuleViolationException {

    public AssetResponsibleCannotAssignInspectionException(Person person) {
        super(person.name() + " is responsible for the asset and cannot assign its inspection");
    }
}
