package ar.edu.itba.certiflow.certification.model;

import java.util.List;

/**
 * Decide si una inspección puede certificarse. Primero aplica lo que ninguna política puede pasar por alto
 * (una inspección abierta, o que no cubre el alcance); si eso se cumple, decide la política de la jurisdicción.
 */
public final class CertificationEligibility {

    private final Requirements<InspectionResult> commonRequirements = new Requirements<>(List.of(
            new Requirement<>(InspectionResult::isClosed, StandardIneligibilityReason.INSPECTION_NOT_CLOSED,
                    result -> "Inspection is still open"),
            new Requirement<>(result -> !result.requiresPartialCertificates(),
                    StandardIneligibilityReason.REQUIRES_PARTIAL_CERTIFICATES,
                    result -> "Asset is split into subsystems: certify each subsystem and derive the global certificate"),
            new Requirement<>(result -> result.requiresPartialCertificates() || result.coversWholeSchema(),
                    StandardIneligibilityReason.PARTIAL_INSPECTION,
                    result -> "Inspection does not cover certification scope")));

    private final CertificationPolicyResolver certificationPolicies;

    public CertificationEligibility(CertificationPolicyResolver certificationPolicies) {
        this.certificationPolicies = certificationPolicies;
    }

    public CertificationDecision decide(CertificationAssessmentContext context) {
        CertificationPolicy policy = certificationPolicies.resolve(context.jurisdictionCode());
        List<Ineligibility> unmetRequirements = commonRequirements.unmetBy(context.result());
        return unmetRequirements.isEmpty()
                ? policy.evaluate(context)
                : new RejectedDecision(unmetRequirements, policy.code(), context.jurisdictionCode(),
                        context.evaluatedAt(), context.result().scope());
    }
}
