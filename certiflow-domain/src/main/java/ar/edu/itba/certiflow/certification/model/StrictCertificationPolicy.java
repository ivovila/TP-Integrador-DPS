package ar.edu.itba.certiflow.certification.model;

public final class StrictCertificationPolicy implements CertificationPolicy {
    @Override public String code() { return "STRICT"; }

    @Override
    public CertificationDecision evaluate(CertificationAssessmentContext context) {
        var reasons = context.result().openNonConformities().stream()
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
