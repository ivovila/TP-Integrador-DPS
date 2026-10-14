package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import java.time.Instant;
import java.util.List;

public record RejectedDecision(List<Ineligibility> ineligibilities, PolicyCode policyCode, JurisdictionCode jurisdictionCode,
                               Instant evaluatedAt, CertificationScope scope) implements CertificationDecision {

    public RejectedDecision {
        ineligibilities = List.copyOf(ineligibilities);
    }

    @Override
    public CertificationOutcome outcome() {
        return CertificationOutcome.REJECTED;
    }

    @Override
    public boolean isEligible() {
        return false;
    }

    @Override
    public GrantedDecision ensureGranted() {
        throw new NotEligibleForCertificationException(this);
    }

    @Override
    public <T> T describedBy(CertificationDecisionDescriber<T> describer) {
        return describer.rejected(this);
    }
}
