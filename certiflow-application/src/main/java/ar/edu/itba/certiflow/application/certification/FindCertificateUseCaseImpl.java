package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateView;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import java.time.Clock;

public final class FindCertificateUseCaseImpl implements FindCertificateUseCase {

    private final CertificateRepository certificateRepository;
    private final CertificateStatuses statuses;

    public FindCertificateUseCaseImpl(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.statuses = new CertificateStatuses(certificateRepository, clock);
    }

    @Override
    public CertificateView execute(CertificateNumber number) {
        return statuses.viewOf(certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number)));
    }
}
