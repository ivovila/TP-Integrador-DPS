package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.application.certificate.CertificateNumbering;
import ar.edu.itba.certiflow.domain.certificate.CertificateNumber;
import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialCertificateNumbering implements CertificateNumbering {

    private static final String FORMAT = "CERT-%05d";

    private final AtomicInteger last = new AtomicInteger();

    @Override
    public CertificateNumber next() {
        return new CertificateNumber(FORMAT.formatted(last.incrementAndGet()));
    }
}
