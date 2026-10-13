package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.CertificationAssessmentContext;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.IneligibilityReason;
import ar.edu.itba.certiflow.certification.model.Requirement;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

public final class CertificationAssessmentService {

    /** Guardas comunes: ninguna política puede aprobar una inspección abierta o que no cubre el alcance. */
    private static final List<Requirement<InspectionResult>> COMMON_REQUIREMENTS = List.of(
            Requirement.that(InspectionResult::isClosed, IneligibilityReason.INSPECTION_NOT_CLOSED,
                    result -> "Inspection is still open"),
            Requirement.that(result -> !result.requiresPartialCertificates(), IneligibilityReason.REQUIRES_PARTIAL_CERTIFICATES,
                    result -> "Asset is split into subsystems: certify each subsystem and derive the global certificate"),
            Requirement.that(result -> result.requiresPartialCertificates() || result.coversWholeSchema(),
                    IneligibilityReason.PARTIAL_INSPECTION, result -> "Inspection does not cover certification scope"));

    private final AssetRepository assets;
    private final CertificationPolicyResolver policies;
    private final Clock clock;

    public CertificationAssessmentService(AssetRepository assets,
            CertificationPolicyResolver policies, Clock clock) {
        this.assets = assets;
        this.policies = policies;
        this.clock = clock;
    }

    public CertificationDecision evaluate(InspectionResult result) {
        var asset = assets.findByCode(result.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(result.assetCode()));
        var policy = policies.resolve(asset.jurisdictionCode());
        var now = StoredInstant.normalize(clock.instant());
        var context = new CertificationAssessmentContext(result, asset.jurisdictionCode(),
                now, LocalDate.ofInstant(now, clock.getZone()));
        var reasons = Requirement.unmetBy(COMMON_REQUIREMENTS, result);
        return reasons.isEmpty() ? policy.evaluate(context) : CertificationDecision.rejected(context, policy.code(), reasons);
    }

}
