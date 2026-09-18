package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.application.certificate.CertificateRepository;
import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

public final class InMemoryCertificateRepository implements CertificateRepository {

    private final Set<Certificate> certificates = new LinkedHashSet<>();

    @Override
    public void save(Certificate certificate) {
        certificates.add(certificate);
    }

    @Override
    public Optional<Certificate> findCurrentFor(Asset asset, LocalDate date) {
        return certificates.stream()
                .filter(certificate -> certificate.certifies(asset))
                .filter(certificate -> certificate.isCurrentOn(date))
                .findFirst();
    }
}
