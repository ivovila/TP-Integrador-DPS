package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.GlobalCertificationPolicy;
import ar.edu.itba.certiflow.certification.model.GrantedDecision;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.model.exceptions.NotEligibleForCertificationException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import ar.edu.itba.certiflow.certification.model.DecidedGlobalCertification;
import ar.edu.itba.certiflow.certification.model.GlobalAssessmentContext;

/**
 * Decide si un conjunto de parciales respalda el certificado global de un activo. Emitir y renovar un global
 * comparten esta decisión y se diferencian en lo que escriben, así que acá no se escribe nada.
 */
public final class GlobalCertificationService {

    private final AssetRepository assetRepository;
    private final InspectionSchemaRepository inspectionSchemaRepository;
    private final CertificateRepository certificateRepository;
    private final GlobalCertificationPolicy globalCertificationPolicy;
    private final Clock clock;

    public GlobalCertificationService(AssetRepository assetRepository, InspectionSchemaRepository inspectionSchemaRepository,
                                      CertificateRepository certificateRepository,
                                      GlobalCertificationPolicy globalCertificationPolicy, Clock clock) {
        this.assetRepository = assetRepository;
        this.inspectionSchemaRepository = inspectionSchemaRepository;
        this.certificateRepository = certificateRepository;
        this.globalCertificationPolicy = globalCertificationPolicy;
        this.clock = clock;
    }

    public DecidedGlobalCertification decide(AssetCode assetCode, int schemaVersionNumber,
                                             List<CertificateNumber> partialNumbers) {
        Asset asset = assetRepository.findByCode(assetCode).orElseThrow(() -> new AssetNotFoundException(assetCode));
        SchemaVersion version = versionOf(asset, schemaVersionNumber);
        List<Certificate> partials = partialNumbers.stream().map(this::certificateWithNumber).toList();
        Instant decidedAt = StoredInstant.normalize(clock.instant());
        LocalDate on = LocalDate.ofInstant(decidedAt, clock.getZone());
        SchemaReference reference = new SchemaReference(asset.type(), schemaVersionNumber);
        CertificationDecision decision = globalCertificationPolicy.evaluate(new GlobalAssessmentContext(assetCode, reference,
                version.requiredSubsystems(), partials, asset.jurisdictionCode(), on, decidedAt));
        GrantedDecision grantedDecision = decision.granted()
                .orElseThrow(() -> new NotEligibleForCertificationException(decision));
        return new DecidedGlobalCertification(assetCode, reference, version.requiredSubsystems(), partialNumbers,
                globalCertificationPolicy.code(), latestInspectionClosedAt(partials), grantedDecision);
    }

    private SchemaVersion versionOf(Asset asset, int number) {
        return inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()))
                .version(number);
    }

    private Certificate certificateWithNumber(CertificateNumber number) {
        return certificateRepository.findByNumber(number).orElseThrow(() -> new CertificateNotFoundException(number));
    }

    /** La base de inspección del global es la más reciente de sus respaldos: es lo que una renovación debe superar. */
    private static Instant latestInspectionClosedAt(List<Certificate> partials) {
        return partials.stream().map(Certificate::latestInspectionClosedAt).max(Instant::compareTo).orElseThrow();
    }
}
