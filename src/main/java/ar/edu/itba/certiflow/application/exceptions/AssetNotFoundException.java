package ar.edu.itba.certiflow.application.exceptions;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.shared.DomainException;

public class AssetNotFoundException extends DomainException {

    public AssetNotFoundException(AssetCode id) {
        super("Asset " + id.value() + " does not exist");
    }
}
