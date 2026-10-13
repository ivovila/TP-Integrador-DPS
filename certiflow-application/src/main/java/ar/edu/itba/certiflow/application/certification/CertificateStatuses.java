package ar.edu.itba.certiflow.application.certification;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateStatus;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import ar.edu.itba.certiflow.certification.usecase.CertificateView;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Resuelve el estado de los certificados contra los demás certificados de su activo. */
final class CertificateStatuses {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    CertificateStatuses(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    CertificateStatus statusOf(Certificate certificate, LocalDate date) {
        return certificate.statusOn(date, certificateRepository.findByAsset(certificate.assetCode()));
    }

    CertificateView viewOf(Certificate certificate) {
        return new CertificateView(certificate, statusOf(certificate, LocalDate.now(clock)));
    }

    /** Certificados de un mismo activo: la lista ya trae todo lo necesario para resolver sus estados. */
    List<CertificateView> viewsOfOneAsset(List<Certificate> assetCertificates) {
        LocalDate today = LocalDate.now(clock);
        return assetCertificates.stream()
                .map(certificate -> new CertificateView(certificate, certificate.statusOn(today, assetCertificates)))
                .toList();
    }

    List<CertificateView> viewsOf(List<Certificate> certificates) {
        Map<AssetCode, List<Certificate>> byAsset = certificates.stream()
                .collect(Collectors.groupingBy(Certificate::assetCode));
        LocalDate today = LocalDate.now(clock);
        return certificates.stream()
                .map(certificate -> new CertificateView(certificate,
                        certificate.statusOn(today, byAsset.get(certificate.assetCode()))))
                .toList();
    }
}
