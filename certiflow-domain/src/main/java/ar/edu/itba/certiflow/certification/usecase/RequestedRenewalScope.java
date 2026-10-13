package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationScope;

/** El alcance que pide quien renueva: el mismo del certificado, o uno explícito que tiene que coincidir. */
public interface RequestedRenewalScope {

    CertificationScope resolveAgainst(CertificationScope currentScope);
}
