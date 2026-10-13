package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class AssetLacksRequiredSubsystemsException extends InvalidValueException {

    public AssetLacksRequiredSubsystemsException(AssetCode assetCode) {
        super("Asset " + assetCode.value() + " lacks subsystems that the schema version requires");
    }
}
