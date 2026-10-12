package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;

public final class FindCertificateUseCaseImpl implements FindCertificateUseCase {

    private final CertificateRepository certificateRepository;

    public FindCertificateUseCaseImpl(CertificateRepository certificateRepository) {
        this.certificateRepository = certificateRepository;
    }

    @Override
    public Certificate execute(CertificateNumber number) {
        return certificateRepository.findByNumber(number).orElseThrow(() -> new CertificateNotFoundException(number));
    }
}
