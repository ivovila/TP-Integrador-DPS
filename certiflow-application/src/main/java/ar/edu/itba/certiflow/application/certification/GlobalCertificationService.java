package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationDecision;
import ar.edu.itba.certiflow.certification.model.DecidedGlobalCertification;
import ar.edu.itba.certiflow.certification.model.GlobalAssessmentContext;
import ar.edu.itba.certiflow.certification.model.GlobalCertificationPolicy;
import ar.edu.itba.certiflow.certification.model.SchemaReference;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.schema.model.exceptions.NoInspectionSchemaForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
        Instant decidedAt = clock.instant();
        GlobalAssessmentContext context = new GlobalAssessmentContext(assetCode,
                new SchemaReference(asset.type(), schemaVersionNumber), version.requiredSubsystems(), partials,
                asset.jurisdictionCode(), LocalDate.ofInstant(decidedAt, clock.getZone()), decidedAt);
        CertificationDecision decision = globalCertificationPolicy.evaluate(context);
        return context.decidedWith(decision.ensureGranted(), globalCertificationPolicy.code());
    }

    private SchemaVersion versionOf(Asset asset, int number) {
        return inspectionSchemaRepository.findByAssetType(asset.type())
                .orElseThrow(() -> new NoInspectionSchemaForAssetTypeException(asset.type()))
                .version(number);
    }

    private Certificate certificateWithNumber(CertificateNumber number) {
        return certificateRepository.findByNumber(number).orElseThrow(() -> new CertificateNotFoundException(number));
    }
}
