package ar.edu.itba.certiflow.certification.model;

import static ar.edu.itba.certiflow.certification.model.StandardFindingTreatment.BLOCKS;
import static ar.edu.itba.certiflow.certification.model.StandardFindingTreatment.REQUIRES_CONDITION;
import static ar.edu.itba.certiflow.certification.model.StandardFindingTreatment.TOLERATED;
import static ar.edu.itba.certiflow.schema.model.StandardSeverity.CRITICAL;
import static ar.edu.itba.certiflow.schema.model.StandardSeverity.MAJOR;
import static ar.edu.itba.certiflow.schema.model.StandardSeverity.MINOR;

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
    private final ValidityTerm conditionalTerm;

    public SeverityCertificationPolicy(String code, Map<Severity, FindingTreatment> treatments, ValidityTerm approvedTerm,
                                       ValidityTerm conditionalTerm) {
        this.code = code;
        this.treatments = Map.copyOf(treatments);
        this.approvedTerm = approvedTerm;
        this.conditionalTerm = conditionalTerm;
    }

    /** Cualquier hallazgo abierto rechaza; aprueba por 12 meses. */
    public static SeverityCertificationPolicy strict() {
        return new SeverityCertificationPolicy("STRICT", Map.of(MINOR, BLOCKS, MAJOR, BLOCKS, CRITICAL, BLOCKS),
                ValidityTerm.months(12), ValidityTerm.notGranted());
    }

    /** MAJOR y CRITICAL rechazan, MINOR se tolera; aprueba por 12 meses. */
    public static SeverityCertificationPolicy standard() {
        return new SeverityCertificationPolicy("STANDARD", Map.of(MINOR, TOLERATED, MAJOR, BLOCKS, CRITICAL, BLOCKS),
                ValidityTerm.months(12), ValidityTerm.notGranted());
    }

    /** CRITICAL rechaza; MINOR o MAJOR dan un certificado condicional por 30 días; sin hallazgos aprueba por 6 meses. */
    public static SeverityCertificationPolicy conditional() {
        return new SeverityCertificationPolicy("CONDITIONAL",
                Map.of(MINOR, REQUIRES_CONDITION, MAJOR, REQUIRES_CONDITION, CRITICAL, BLOCKS),
                ValidityTerm.months(6), ValidityTerm.days(30));
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public CertificationDecision evaluate(CertificationAssessmentContext context) {
        PolicyAssessment assessment = new PolicyAssessment(context, code, approvedTerm, conditionalTerm);
        context.result().openNonConformities().forEach(finding ->
                treatments.getOrDefault(finding.severity(), StandardFindingTreatment.UNSUPPORTED).applyTo(finding, assessment));
        return assessment.decision();
    }
}
