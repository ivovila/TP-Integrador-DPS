package ar.edu.itba.certiflow.models.asset.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class AssetAlreadyRegisteredException extends DomainException {

    public AssetAlreadyRegisteredException(AssetCode code) {
        super("An asset with code " + code.value() + " is already registered");
    }
}
