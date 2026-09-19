package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.application.report.CertificateDocument;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import java.time.Clock;
import java.time.LocalDate;

public final class GenerateCertificateDocument {

    private final Clock clock;

    public GenerateCertificateDocument(Clock clock) {
        this.clock = clock;
    }

    public CertificateDocument execute(Certificate certificate) {
        LocalDate today = LocalDate.now(clock);
        Asset asset = certificate.asset();
        return new CertificateDocument(certificate.number(), asset.code(), asset.name(), certificate.validity(), today,
                certificate.statusOn(today), certificate.issuedBy(), certificate.issuedAt(),
                certificate.suspensions());
    }
}
