package ar.edu.itba.certiflow.details.inmemory;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import ar.edu.itba.certiflow.models.certificate.CertificateNumber;
import ar.edu.itba.certiflow.ports.CertificateRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryCertificateRepository implements CertificateRepository {

    private final Map<CertificateNumber, Certificate> certificates = new LinkedHashMap<>();

    @Override
    public void save(Certificate certificate) {
        certificates.put(certificate.number(), certificate);
    }

    @Override
    public Optional<Certificate> findByNumber(CertificateNumber number) {
        return Optional.ofNullable(certificates.get(number));
    }

    @Override
    public Optional<Certificate> findCurrentFor(Asset asset, LocalDate referenceDate) {
        return certificates.values().stream()
                .filter(certificate -> certificate.certifies(asset))
                .filter(certificate -> certificate.isCurrentOn(referenceDate))
                .findFirst();
    }
}
