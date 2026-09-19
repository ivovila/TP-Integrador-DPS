package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.ports.CertificateNumbering;
import java.util.concurrent.atomic.AtomicInteger;

public final class SequentialCertificateNumbering implements CertificateNumbering {

    private static final String NUMBER_FORMAT = "CERT-%05d";

    private final AtomicInteger lastIssuedNumber = new AtomicInteger();

    @Override
    public CertificateNumber next() {
        return new CertificateNumber(NUMBER_FORMAT.formatted(lastIssuedNumber.incrementAndGet()));
    }
}
