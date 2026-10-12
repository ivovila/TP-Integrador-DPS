package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.DomainException;
import ar.edu.itba.certiflow.shared.model.Person;

public class AssetResponsibleCannotInspectOwnAssetException extends DomainException {

    public AssetResponsibleCannotInspectOwnAssetException(Person person) {
        super(person.name() + " is responsible for the asset and cannot inspect it");
    }
}
