package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.certificate.CertificateGenerator;
import ar.edu.itba.certiflow.domain.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.LocalDate;

public final class IssueCertificate {

    private final CertificateRepository certificates;
    private final CertificationEligibility eligibility;
    private final CertificateNumbering numbering;
    private final CertificateGenerator certificateGenerator;
    private final Clock clock;

    public IssueCertificate(CertificateRepository certificates, CertificationEligibility eligibility,
                            CertificateNumbering numbering, CertificateGenerator certificateGenerator,
                            Clock clock) {
        this.certificates = certificates;
        this.eligibility = eligibility;
        this.numbering = numbering;
        this.certificateGenerator = certificateGenerator;
        this.clock = clock;
    }

    public Certificate execute(Inspection inspection, LocalDate validUntil, Person issuedBy) {
        LocalDate today = LocalDate.now(clock);
        Asset asset = inspection.asset();
        eligibility.ensureEligible(inspection);
        certificates.findCurrentFor(asset, today).ifPresent(current -> {
            throw new AssetAlreadyCertifiedException(asset, current.number());
        });
        Certificate certificate = certificateGenerator.generate(numbering.next(), asset, inspection,
                new ValidityPeriod(today, validUntil), issuedBy, clock.instant());
        certificates.save(certificate);
        return certificate;
    }
}
