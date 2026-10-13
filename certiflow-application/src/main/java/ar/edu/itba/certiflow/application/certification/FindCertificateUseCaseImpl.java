package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.certification.model.AssetCertificates;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateView;
import ar.edu.itba.certiflow.certification.usecase.FindCertificateUseCase;
import java.time.Clock;
import java.time.LocalDate;

public final class FindCertificateUseCaseImpl implements FindCertificateUseCase {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public FindCertificateUseCaseImpl(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    @Override
    public CertificateView execute(CertificateNumber number) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        AssetCertificates assetCertificates = new AssetCertificates(certificate.assetCode(),
                certificateRepository.findByAsset(certificate.assetCode()));
        return new CertificateView(certificate, assetCertificates.statusOf(certificate, LocalDate.now(clock)));
    }
}
