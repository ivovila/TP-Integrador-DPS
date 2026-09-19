package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;

public final class SuspendCertificate {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public SuspendCertificate(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    public void execute(Certificate certificate, String reason, Person suspendedBy) {
        certificate.suspend(reason, suspendedBy, clock.instant());
        certificateRepository.save(certificate);
    }
}
