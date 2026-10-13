package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.shared.model.DomainException;

public final class JurisdictionPolicyNotFoundException extends DomainException {
    public JurisdictionPolicyNotFoundException(JurisdictionCode code) {
        super("No certification policy configured for " + code.value());
    }
}
