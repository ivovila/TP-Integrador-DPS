package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import java.time.Instant;

public interface CertificationDecision {

    CertificationOutcome outcome();

    boolean isEligible();

    /**
     * La decisión otorgada con la que se puede emitir un certificado. Una decisión rechazada no certifica:
     * responde con {@link NotEligibleForCertificationException}, que lleva sus motivos.
     */
    GrantedDecision ensureGranted();

    PolicyCode policyCode();

    JurisdictionCode jurisdictionCode();

    Instant evaluatedAt();

    CertificationScope scope();

    <T> T describedBy(CertificationDecisionDescriber<T> describer);
}
