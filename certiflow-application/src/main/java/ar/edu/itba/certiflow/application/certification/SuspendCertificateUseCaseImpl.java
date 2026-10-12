package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.usecase.SuspendCertificateCommand;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateService;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
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
    public void execute(SuspendCertificateCommand command) {
        Certificate certificate = certificateRepository.findByNumber(command.number())
                .orElseThrow(() -> new CertificateNotFoundException(command.number()));
        certificateService.suspend(certificate, command.reason(), command.suspendedBy(), clock.instant(), LocalDate.now(clock));
        certificateRepository.save(certificate);
    }
}
