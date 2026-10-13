package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.schema.model.StandardSeverity;

public final class StandardCertificationPolicy implements CertificationPolicy {
    @Override public String code() { return "STANDARD"; }

    @Override
    public CertificationDecision evaluate(CertificationAssessmentContext context) {
        var reasons = context.result().openNonConformities().stream()
                .filter(finding -> !StandardSeverity.MINOR.equals(finding.severity()))
                .map(finding -> new Ineligibility(IneligibilityReason.BLOCKING_FINDINGS,
                        "Open finding " + finding.criterionCode() + " severity " + finding.severity().label()))
                .toList();
        if (!reasons.isEmpty()) {
            return CertificationDecision.rejected(context, code(), reasons);
        }
        var from = context.issuanceDate();
        return CertificationDecision.approved(context, code(),
                new ValidityPeriod(from, from.plusMonths(12).minusDays(1)));
    }
}
