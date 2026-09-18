package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public final class GenerateCertificateDocument {

    private final Clock clock;

    public GenerateCertificateDocument(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** El estado se calcula a la fecha de emisión del documento: un mismo certificado puede figurar vigente hoy y vencido mañana. */
    public CertificateDocument execute(Certificate certificate) {
        LocalDate today = LocalDate.now(clock);
        Asset asset = certificate.asset();
        return new CertificateDocument(certificate.number(), asset.code(), asset.name(), certificate.validity(), today,
                certificate.statusOn(today), certificate.issuedBy(), certificate.issuedAt(),
                certificate.suspensions());
    }
}
