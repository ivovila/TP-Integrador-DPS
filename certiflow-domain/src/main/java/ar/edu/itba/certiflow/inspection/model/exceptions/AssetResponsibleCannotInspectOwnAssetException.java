package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import ar.edu.itba.certiflow.shared.model.Person;

public class AssetResponsibleCannotInspectOwnAssetException extends RuleViolationException {

    public AssetResponsibleCannotInspectOwnAssetException(Person person) {
        super(person.name() + " is responsible for the asset and cannot inspect it");
    }
}
