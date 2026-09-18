package ar.edu.itba.certiflow.application.asset;

import ar.edu.itba.certiflow.domain.asset.AssetCode;
import ar.edu.itba.certiflow.domain.shared.DomainException;

public class AssetAlreadyRegisteredException extends DomainException {

    public AssetAlreadyRegisteredException(AssetCode code) {
        super("An asset with code " + code.value() + " is already registered");
    }
}
