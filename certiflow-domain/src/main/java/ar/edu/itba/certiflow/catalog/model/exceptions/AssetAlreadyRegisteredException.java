package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public class AssetAlreadyRegisteredException extends RuleViolationException {

    public AssetAlreadyRegisteredException(AssetCode code) {
        super("An asset with code " + code.value() + " is already registered");
    }
}
