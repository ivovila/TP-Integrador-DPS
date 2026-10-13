package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record RejectedDecision(List<Ineligibility> ineligibilities, String policyCode, JurisdictionCode jurisdictionCode,
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
    public List<CertificationCondition> conditions() {
        return List.of();
    }

    @Override
    public Optional<GrantedDecision> granted() {
        return Optional.empty();
    }

    @Override
    public <T> T describedBy(CertificationDecisionDescriber<T> describer) {
        return describer.rejected(this);
    }
}
