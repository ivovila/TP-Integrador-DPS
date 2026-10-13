package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.*;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.*;
import ar.edu.itba.certiflow.certification.model.exceptions.*;
import ar.edu.itba.certiflow.certification.repository.*;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.StoredInstant;
import java.time.*;
import java.util.*;

public final class GlobalCertificates {
    private final AssetRepository assets;
    private final InspectionSchemaRepository schemas;
    private final CertificateRepository certificates;
    private final CertificateNumbering numbering;
    private final CertificateService service;
    private final GlobalCertificationPolicy policy;
    private final Clock clock;

    public GlobalCertificates(AssetRepository assets, InspectionSchemaRepository schemas, CertificateRepository certificates,
            CertificateNumbering numbering, CertificateService service, GlobalCertificationPolicy policy, Clock clock) {
        this.assets = assets; this.schemas = schemas; this.certificates = certificates;
        this.numbering = numbering; this.service = service; this.policy = policy; this.clock = clock;
    }

    public CertificateNumber issue(AssetCode code, int versionNumber, List<CertificateNumber> numbers,
            Person by, Optional<CertificateNumber> renewing) {
        var asset = assets.findByCode(code).orElseThrow(() -> new AssetNotFoundException(code));
        var version = schemas.findByAssetType(asset.type()).orElseThrow().versions().stream()
                .filter(item -> item.number() == versionNumber).findFirst()
                .orElseThrow(() -> new CertificationConflictException("Schema version not found"));
        var partials = numbers.stream().map(number -> certificates.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number))).toList();
        var now = StoredInstant.normalize(clock.instant());
        var today = LocalDate.ofInstant(now, clock.getZone());
        var reference = new SchemaReference(asset.type(), versionNumber);
        var decision = policy.evaluate(code, reference, version.requiredSubsystems(), partials, asset.jurisdictionCode(), today, now);
        if (!decision.isEligible()) { throw new NotEligibleForCertificationException(decision); }
        var latest = partials.stream().map(Certificate::latestInspectionClosedAt).max(Instant::compareTo).orElseThrow();
        Optional<Certificate> current = renewing.map(number -> certificates.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number)));
        if (current.isPresent()) {
            var old = current.orElseThrow();
            if (!(old.basis() instanceof CertificateBasis.PartialCertificatesBasis) || !old.assetCode().equals(code)
                    || !latest.isAfter(old.latestInspectionClosedAt()) || old.statusOn(today) == CertificateStatus.RENEWED) {
                throw new CertificationConflictException("Global renewal requires matching asset and later inspection basis");
            }
        }
        certificates.findCurrentFor(code, new CertificationScope.WholeAsset(), today).ifPresent(existing -> {
            if (current.isEmpty() || !current.orElseThrow().number().equals(existing.number())) {
                throw new AssetAlreadyCertifiedException(code, existing.number());
            }
        });
        var basis = new CertificateBasis.PartialCertificatesBasis(numbers, policy.code(), latest);
        var certificate = service.issueGlobal(numbering.next(), code, basis, reference, version.requiredSubsystems(), decision, by, now);
        current.ifPresent(old -> { service.markRenewed(old, certificate, by, now); certificates.save(old); });
        certificates.save(certificate);
        return certificate.number();
    }
}
