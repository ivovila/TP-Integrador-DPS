package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class SuspendCertificateUseCaseImpl implements SuspendCertificateUseCase {

    private final CertificateRepository certificateRepository;
    private final CertificateService certificateService;
    private final Clock clock;

    public SuspendCertificateUseCaseImpl(CertificateRepository certificateRepository, CertificateService certificateService, Clock clock) {
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
