package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class RenewCertificate {

    private final CertificateRepository certificates;
    private final CertificationEligibility eligibility;
    private final CertificateNumbering numbering;
    private final Clock clock;

    public RenewCertificate(CertificateRepository certificates, CertificationEligibility eligibility,
                            CertificateNumbering numbering, Clock clock) {
        this.certificates = Objects.requireNonNull(certificates, "certificates");
        this.eligibility = Objects.requireNonNull(eligibility, "eligibility");
        this.numbering = Objects.requireNonNull(numbering, "numbering");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Renovar exige una inspección nueva y cerrada; el certificado anterior queda reemplazado, no borrado. */
    public Certificate execute(Certificate current, Inspection inspection, LocalDate validUntil, Person renewedBy) {
        eligibility.ensureEligible(inspection);
        Certificate renewal = current.renew(numbering.next(), new ValidityPeriod(LocalDate.now(clock), validUntil),
                inspection, renewedBy, clock.instant());
        certificates.save(current);
        certificates.save(renewal);
        return renewal;
    }
}
