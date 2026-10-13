package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.schema.model.StandardSeverity;
import java.util.Set;

public final class ConditionalCertificationPolicy implements CertificationPolicy {
    private static final Set<Severity> ADMITTED = Set.of(StandardSeverity.MINOR, StandardSeverity.MAJOR);

    @Override public String code() { return "CONDITIONAL"; }

    @Override
    public CertificationDecision evaluate(CertificationAssessmentContext context) {
        var findings = context.result().openNonConformities();
        var reasons = findings.stream().filter(finding -> !ADMITTED.contains(finding.severity()))
                .map(finding -> new Ineligibility(StandardSeverity.CRITICAL.equals(finding.severity())
                        ? IneligibilityReason.BLOCKING_FINDINGS : IneligibilityReason.UNSUPPORTED_SEVERITY,
                        "Open finding " + finding.criterionCode() + " severity " + finding.severity().label()))
                .toList();
        if (!reasons.isEmpty()) {
            return CertificationDecision.rejected(context, code(), reasons);
        }
        var from = context.issuanceDate();
        if (findings.isEmpty()) {
            return CertificationDecision.approved(context, code(),
                    new ValidityPeriod(from, from.plusMonths(6).minusDays(1)));
        }
        var until = from.plusDays(29);
        var conditions = findings.stream().map(finding -> new CertificationCondition(finding.findingId(),
                finding.criterionCode(), "Resolve and verify open nonconformity", until)).toList();
        return CertificationDecision.conditional(context, code(), new ValidityPeriod(from, until), conditions);
    }
}
