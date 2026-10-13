package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationScope;

public record SameScopeAsCurrent() implements RequestedRenewalScope {

    @Override
    public CertificationScope resolveAgainst(CertificationScope currentScope) {
        return currentScope;
    }
}
