package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.CertificationAssessmentContext;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationPolicyResolver;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import ar.edu.itba.certiflow.certification.model.Ineligibility;
import ar.edu.itba.certiflow.certification.model.IneligibilityReason;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CertificationAssessmentService {
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
        var reasons = unmetRequirements(result);
        if (!reasons.isEmpty()) {
            return CertificationDecision.rejected(context, policy.code(), reasons);
        }
        return policy.evaluate(context);
    }

    private List<Ineligibility> unmetRequirements(InspectionResult result) {
        List<Ineligibility> reasons = new ArrayList<>();
        if (!result.isClosed()) {
            reasons.add(new Ineligibility(IneligibilityReason.INSPECTION_NOT_CLOSED,
                    "Inspection is still open"));
        }
        if (!result.coversWholeSchema()) {
            reasons.add(new Ineligibility(IneligibilityReason.PARTIAL_INSPECTION,
                    "Inspection does not cover certification scope"));
        }
        return List.copyOf(reasons);
    }

}
