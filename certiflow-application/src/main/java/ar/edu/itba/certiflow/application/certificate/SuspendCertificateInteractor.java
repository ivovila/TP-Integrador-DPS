package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.CertificateService;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.models.shared.Person;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import ar.edu.itba.certiflow.usecases.certificate.SuspendCertificate;
import java.time.Clock;
import java.time.LocalDate;

public final class SuspendCertificateInteractor implements SuspendCertificate {

    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final Clock clock;

    public SuspendCertificateInteractor(CertificateRepository certificateRepository, CertificateService certificateService, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.certificateService = certificateService;
        this.clock = clock;
    }

    @Override
    public void execute(CertificateNumber number, String reason, Person suspendedBy) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        certificateService.suspend(certificate, reason, suspendedBy, clock.instant(), LocalDate.now(clock));
        certificateRepository.save(certificate);
    }
}
