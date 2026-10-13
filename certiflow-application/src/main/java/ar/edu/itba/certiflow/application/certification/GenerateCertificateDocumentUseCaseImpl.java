package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.certification.model.AssetCertificates;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateDocument;
import ar.edu.itba.certiflow.certification.usecase.GenerateCertificateDocumentUseCase;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Clock;
import java.time.LocalDate;

public final class GenerateCertificateDocumentUseCaseImpl implements GenerateCertificateDocumentUseCase {

    private final CertificateRepository certificateRepository;
    private final AssetRepository assetRepository;
    private final Clock clock;

    public GenerateCertificateDocumentUseCaseImpl(CertificateRepository certificateRepository, AssetRepository assetRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.assetRepository = assetRepository;
        this.clock = clock;
    }

    @Override
    public CertificateDocument execute(CertificateNumber number) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        LocalDate today = LocalDate.now(clock);
        Asset asset = assetRepository.findByCode(certificate.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(certificate.assetCode()));
        AssetCertificates assetCertificates = new AssetCertificates(certificate.assetCode(),
                certificateRepository.findByAsset(certificate.assetCode()));
        return new CertificateDocument(certificate.number(), asset.code(), asset.name(), certificate.validity(), today,
                assetCertificates.statusOf(certificate, today), new Stamp(certificate.issuedBy(), certificate.issuedAt()),
                certificate.suspensions(), certificate.decision(), certificate.scope(), certificate.basis());
    }
}
