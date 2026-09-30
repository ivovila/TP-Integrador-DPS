package ar.edu.itba.certiflow.usecases.certificate;

import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class SuspendCertificate {

    private final CertificateRepository certificateRepository;
    private final AuditLog<Certificate> certificateLog;
    private final Clock clock;

    public SuspendCertificate(CertificateRepository certificateRepository, AuditLog<Certificate> certificateLog, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.certificateLog = certificateLog;
        this.clock = clock;
    }

    public void execute(CertificateNumber number, String reason, Person suspendedBy) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        certificate.suspend(reason, suspendedBy, clock.instant(), LocalDate.now(clock));
        certificateRepository.save(certificate);
        certificateLog.recordAll(certificate, certificate.pullAuditEntries());
    }
}
