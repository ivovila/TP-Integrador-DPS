package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.DomainException;

public class AssetNotFoundException extends DomainException {

    public AssetNotFoundException(AssetCode id) {
        super("Asset " + id.value() + " does not exist");
    }
}
