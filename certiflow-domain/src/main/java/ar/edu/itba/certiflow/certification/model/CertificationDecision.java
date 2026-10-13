package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.JurisdictionCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record CertificationDecision(CertificationOutcome outcome, List<Ineligibility> ineligibilities,
        List<CertificationCondition> conditions, Optional<ValidityPeriod> validity,
        String policyCode,
        JurisdictionCode jurisdictionCode, Instant evaluatedAt, CertificationScope scope) {
    public CertificationDecision {
        ineligibilities = List.copyOf(ineligibilities);
        conditions = List.copyOf(conditions);
        if ((outcome != CertificationOutcome.REJECTED) != validity.isPresent()) {
            throw new IllegalArgumentException("Only eligible decisions have validity");
        }
        if (outcome == CertificationOutcome.CONDITIONAL && conditions.isEmpty()) {
            throw new IllegalArgumentException("Conditional decisions require conditions");
        }
    }

    public static CertificationDecision rejected(CertificationAssessmentContext context,
            String policy, List<Ineligibility> reasons) {
        return new CertificationDecision(CertificationOutcome.REJECTED, reasons, List.of(),
                Optional.empty(), policy, context.jurisdictionCode(), context.evaluatedAt(), context.result().scope());
    }

    public static CertificationDecision approved(CertificationAssessmentContext context,
            String policy, ValidityPeriod validity) {
        return new CertificationDecision(CertificationOutcome.APPROVED, List.of(), List.of(),
                Optional.of(validity), policy, context.jurisdictionCode(), context.evaluatedAt(), context.result().scope());
    }

    public static CertificationDecision conditional(CertificationAssessmentContext context,
            String policy, ValidityPeriod validity, List<CertificationCondition> conditions) {
        return new CertificationDecision(CertificationOutcome.CONDITIONAL, List.of(), conditions,
                Optional.of(validity), policy, context.jurisdictionCode(), context.evaluatedAt(), context.result().scope());
    }

    /** Concede la vigencia indicada; con condiciones pendientes la modalidad es condicional. */
    public static CertificationDecision granted(List<CertificationCondition> conditions, ValidityPeriod validity, String policy,
            JurisdictionCode jurisdictionCode, Instant evaluatedAt, CertificationScope scope) {
        CertificationOutcome outcome = conditions.isEmpty() ? CertificationOutcome.APPROVED : CertificationOutcome.CONDITIONAL;
        return new CertificationDecision(outcome, List.of(), conditions, Optional.of(validity), policy, jurisdictionCode,
                evaluatedAt, scope);
    }

    public boolean isEligible() { return outcome != CertificationOutcome.REJECTED; }

    public boolean isBlockedBy(IneligibilityReason reason) {
        return ineligibilities.stream().anyMatch(item -> item.reason() == reason);
    }
}
