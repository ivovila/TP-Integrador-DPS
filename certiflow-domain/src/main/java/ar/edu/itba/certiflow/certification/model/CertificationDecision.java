package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CertificationDecision {

    CertificationOutcome outcome();

    boolean isEligible();

    List<Ineligibility> ineligibilities();

    List<CertificationCondition> conditions();

    Optional<GrantedDecision> granted();

    PolicyCode policyCode();

    JurisdictionCode jurisdictionCode();

    Instant evaluatedAt();

    CertificationScope scope();

    <T> T describedBy(CertificationDecisionDescriber<T> describer);
}
