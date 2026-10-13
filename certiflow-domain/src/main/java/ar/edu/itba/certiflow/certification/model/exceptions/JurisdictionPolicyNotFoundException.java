package ar.edu.itba.certiflow.certification.model.exceptions;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.shared.model.RuleViolationException;

public final class JurisdictionPolicyNotFoundException extends RuleViolationException {
    public JurisdictionPolicyNotFoundException(JurisdictionCode code) {
        super("No certification policy configured for " + code.value());
    }
}
