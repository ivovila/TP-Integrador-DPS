package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import java.time.Instant;
import java.util.List;

public record GrantedDecision(List<CertificationCondition> conditions, ValidityPeriod validity, PolicyCode policyCode,
                              JurisdictionCode jurisdictionCode, Instant evaluatedAt, CertificationScope scope)
        implements CertificationDecision {

    public GrantedDecision {
        conditions = List.copyOf(conditions);
    }

    @Override
    public CertificationOutcome outcome() {
        return conditions.isEmpty() ? CertificationOutcome.APPROVED : CertificationOutcome.CONDITIONAL;
    }

    @Override
    public boolean isEligible() {
        return true;
    }

    @Override
    public GrantedDecision ensureGranted() {
        return this;
    }

    @Override
    public <T> T describedBy(CertificationDecisionDescriber<T> describer) {
        return describer.granted(this);
    }
}
