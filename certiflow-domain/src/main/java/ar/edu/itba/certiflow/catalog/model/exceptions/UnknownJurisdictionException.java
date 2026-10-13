package ar.edu.itba.certiflow.catalog.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.shared.model.InvalidValueException;

public class UnknownJurisdictionException extends InvalidValueException {

    public UnknownJurisdictionException(JurisdictionCode jurisdictionCode) {
        super("No certification policy is configured for jurisdiction " + jurisdictionCode.value());
    }
}
