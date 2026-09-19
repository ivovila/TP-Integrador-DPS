package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.ports.CertificateRepository;
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
    public Optional<Certificate> findCurrentFor(Asset asset, LocalDate referenceDate) {
        return certificates.stream()
                .filter(certificate -> certificate.certifies(asset))
                .filter(certificate -> certificate.isCurrentOn(referenceDate))
                .findFirst();
    }
}
