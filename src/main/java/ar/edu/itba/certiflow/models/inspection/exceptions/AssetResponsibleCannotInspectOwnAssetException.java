package ar.edu.itba.certiflow.models.inspection.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import ar.edu.itba.certiflow.models.shared.Person;

public class AssetResponsibleCannotInspectOwnAssetException extends DomainException {

    public AssetResponsibleCannotInspectOwnAssetException(Person person) {
        super(person.name() + " is responsible for the asset and cannot inspect it");
    }
}
