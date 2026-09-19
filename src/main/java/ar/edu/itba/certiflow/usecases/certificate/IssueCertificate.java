package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.application.certification.CertificationEligibility;
import ar.edu.itba.certiflow.application.exceptions.AssetAlreadyCertifiedException;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.AuditedCertificateGenerator;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificate {

    private final CertificateRepository certificateRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final AuditedCertificateGenerator certificateGenerator;
    private final Clock clock;

    public IssueCertificate(CertificateRepository certificateRepository, CertificationEligibility certificationEligibility,
                            CertificateNumbering certificateNumbering, AuditedCertificateGenerator certificateGenerator,
                            Clock clock) {
        this.certificateRepository = certificateRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.certificateGenerator = certificateGenerator;
        this.clock = clock;
    }

    public Certificate execute(Inspection inspection, LocalDate validUntil, Person issuedBy) {
        LocalDate today = LocalDate.now(clock);
        Asset asset = inspection.asset();
        certificationEligibility.ensureEligible(inspection);
        certificateRepository.findCurrentFor(asset, today).ifPresent(currentCertificate -> {
            throw new AssetAlreadyCertifiedException(asset, currentCertificate.number());
        });
        Certificate certificate = certificateGenerator.generate(certificateNumbering.next(), asset, inspection,
                new ValidityPeriod(today, validUntil), issuedBy, clock.instant());
        certificateRepository.save(certificate);
        return certificate;
    }
}
