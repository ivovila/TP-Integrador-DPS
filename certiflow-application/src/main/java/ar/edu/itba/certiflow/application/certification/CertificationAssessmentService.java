package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.CertificationAssessmentContext;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.CertificationEligibility;
import ar.edu.itba.certiflow.certification.model.InspectionResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

/** Carga lo que la decisión necesita y lee el reloj; quién decide es {@link CertificationEligibility}. */
public final class CertificationAssessmentService {

    private final AssetRepository assetRepository;
    private final CertificationEligibility certificationEligibility;
    private final Clock clock;

    public CertificationAssessmentService(AssetRepository assetRepository,
                                          CertificationEligibility certificationEligibility, Clock clock) {
        this.assetRepository = assetRepository;
        this.certificationEligibility = certificationEligibility;
        this.clock = clock;
    }

    public CertificationDecision evaluate(InspectionResult result) {
        Asset asset = assetRepository.findByCode(result.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(result.assetCode()));
        Instant evaluatedAt = clock.instant();
        return certificationEligibility.decide(new CertificationAssessmentContext(result, asset.jurisdictionCode(),
                evaluatedAt, LocalDate.ofInstant(evaluatedAt, clock.getZone())));
    }
}
