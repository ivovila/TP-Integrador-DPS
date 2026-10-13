package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.AssetCertificates;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateView;
import ar.edu.itba.certiflow.certification.usecase.ListCertificatesUseCase;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ListCertificatesUseCaseImpl implements ListCertificatesUseCase {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public ListCertificatesUseCaseImpl(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    /** El listado ya trae todos los certificados de cada activo, que es lo que hace falta para resolver su estado. */
    @Override
    public List<CertificateView> execute() {
        List<Certificate> certificates = certificateRepository.findAll();
        Map<AssetCode, AssetCertificates> certificatesByAsset = certificates.stream()
                .collect(Collectors.groupingBy(Certificate::assetCode))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        assetGroup -> new AssetCertificates(assetGroup.getKey(), assetGroup.getValue())));
        LocalDate today = LocalDate.now(clock);
        return certificates.stream()
                .map(certificate -> new CertificateView(certificate,
                        certificatesByAsset.get(certificate.assetCode()).statusOf(certificate, today)))
                .toList();
    }

    @Override
    public List<CertificateView> execute(AssetCode assetCode) {
        AssetCertificates assetCertificates = new AssetCertificates(assetCode,
                certificateRepository.findByAsset(assetCode));
        LocalDate today = LocalDate.now(clock);
        return assetCertificates.certificates().stream()
                .map(certificate -> new CertificateView(certificate, assetCertificates.statusOf(certificate, today)))
                .toList();
    }
}
