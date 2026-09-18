package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.certificate.Certificate;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Clock;

public final class SuspendCertificate {

    private final CertificateRepository certificates;
    private final Clock clock;

    public SuspendCertificate(CertificateRepository certificates, Clock clock) {
        this.certificates = certificates;
        this.clock = clock;
    }

    public void execute(Certificate certificate, String reason, Person suspendedBy) {
        certificate.suspend(reason, suspendedBy, clock.instant());
        certificates.save(certificate);
    }
}
