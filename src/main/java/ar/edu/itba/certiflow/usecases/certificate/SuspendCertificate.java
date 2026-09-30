package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class SuspendCertificate {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public SuspendCertificate(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    public void execute(CertificateNumber number, String reason, Person suspendedBy) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        certificate.suspend(reason, suspendedBy, clock.instant(), LocalDate.now(clock));
        certificateRepository.save(certificate);
    }
}
