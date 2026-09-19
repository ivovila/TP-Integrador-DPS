package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.application.certification.CertificationEligibility;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class RenewCertificate {

    private final CertificateRepository certificateRepository;
    private final CertificationEligibility certificationEligibility;
    private final CertificateNumbering certificateNumbering;
    private final Clock clock;

    public RenewCertificate(CertificateRepository certificateRepository, CertificationEligibility certificationEligibility,
                            CertificateNumbering certificateNumbering, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.certificationEligibility = certificationEligibility;
        this.certificateNumbering = certificateNumbering;
        this.clock = clock;
    }

    public Certificate execute(Certificate currentCertificate, Inspection inspection, LocalDate validUntil, Person renewedBy) {
        certificationEligibility.ensureEligible(inspection);
        Certificate renewedCertificate = currentCertificate.renew(certificateNumbering.next(), new ValidityPeriod(LocalDate.now(clock), validUntil),
                inspection, renewedBy, clock.instant());
        certificateRepository.save(currentCertificate);
        certificateRepository.save(renewedCertificate);
        return renewedCertificate;
    }
}
