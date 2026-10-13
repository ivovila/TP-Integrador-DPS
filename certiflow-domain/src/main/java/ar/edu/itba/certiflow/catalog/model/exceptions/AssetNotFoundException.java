package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.NotFoundException;

public class AssetNotFoundException extends NotFoundException {

    public AssetNotFoundException(AssetCode assetCode) {
        super("Asset " + assetCode.value() + " does not exist");
    }
}
