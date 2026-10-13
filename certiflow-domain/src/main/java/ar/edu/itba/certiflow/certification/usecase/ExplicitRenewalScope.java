package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.certification.model.exceptions.RenewalScopeMismatchException;

public record ExplicitRenewalScope(CertificationScope requestedScope) implements RequestedRenewalScope {

    @Override
    public CertificationScope resolveAgainst(CertificationScope currentScope) {
        if (!requestedScope.equals(currentScope)) {
            throw new RenewalScopeMismatchException();
        }
        return requestedScope;
    }
}
