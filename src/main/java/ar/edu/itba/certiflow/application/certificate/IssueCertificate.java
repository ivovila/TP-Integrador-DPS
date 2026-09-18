package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.AuditedCertificateGenerator;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class IssueCertificate {

    private final CertificateRepository certificates;
    private final CertificationEligibility eligibility;
    private final CertificateNumbering numbering;
    private final AuditedCertificateGenerator certificateGenerator;
    private final Clock clock;

    public IssueCertificate(CertificateRepository certificates, CertificationEligibility eligibility,
                            CertificateNumbering numbering, AuditedCertificateGenerator certificateGenerator,
                            Clock clock) {
        this.certificates = Objects.requireNonNull(certificates, "certificates");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.numbering = Objects.requireNonNull(numbering, "numbering");
        this.certificateGenerator = Objects.requireNonNull(certificateGenerator, "certificateGenerator");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** La vigencia arranca el día de emisión. Un activo no puede tener dos certificados vigentes a la vez. */
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
