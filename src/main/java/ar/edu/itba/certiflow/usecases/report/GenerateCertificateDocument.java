package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.application.report.CertificateDocument;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.models.certificate.exceptions.CertificateNotFoundException;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.Clock;
import java.time.LocalDate;

public final class GenerateCertificateDocument {

    private final CertificateRepository certificateRepository;
    private final Clock clock;

    public GenerateCertificateDocument(CertificateRepository certificateRepository, Clock clock) {
        this.certificateRepository = certificateRepository;
        this.clock = clock;
    }

    public CertificateDocument execute(CertificateNumber number) {
        Certificate certificate = certificateRepository.findByNumber(number)
                .orElseThrow(() -> new CertificateNotFoundException(number));
        LocalDate today = LocalDate.now(clock);
        Asset asset = certificate.asset();
        return new CertificateDocument(certificate.number(), asset.code(), asset.name(), certificate.validity(), today,
                certificate.statusOn(today), certificate.issuedBy(), certificate.issuedAt(),
                certificate.suspensions());
    }
}
