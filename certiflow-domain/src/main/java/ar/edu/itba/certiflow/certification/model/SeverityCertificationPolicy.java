package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.schema.model.Severity;
import java.util.Map;

/**
 * Política de certificación definida por datos: qué hace con cada severidad abierta y cuánto concede.
 * Las jurisdicciones de demostración son configuraciones de esta clase; otra regla distinta puede ser
 * otra implementación de {@link CertificationPolicy} sin tocar el servicio central.
 */
public final class SeverityCertificationPolicy implements CertificationPolicy {

    private final String code;
    private final Map<Severity, FindingTreatment> treatments;
    private final ValidityTerm approvedTerm;

    public SeverityCertificationPolicy(String code, Map<Severity, FindingTreatment> treatments, ValidityTerm approvedTerm) {
        this.code = code;
        this.treatments = Map.copyOf(treatments);
        this.approvedTerm = approvedTerm;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public CertificationDecision evaluate(CertificationAssessmentContext context) {
        PolicyAssessment assessment = new PolicyAssessment(context, code, approvedTerm);
        context.result().openNonConformities().forEach(finding ->
                treatments.getOrDefault(finding.severity(), StandardFindingTreatment.UNSUPPORTED).applyTo(finding, assessment));
        return assessment.decision();
    }
}
