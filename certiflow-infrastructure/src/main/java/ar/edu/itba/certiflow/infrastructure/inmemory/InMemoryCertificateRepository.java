package ar.edu.itba.certiflow.infrastructure.inmemory;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.repository.CertificateRepository;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
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
    public Optional<Certificate> findCurrentFor(AssetCode assetCode, LocalDate referenceDate) {
        return certificates.values().stream()
                .filter(certificate -> certificate.certifies(assetCode))
                .filter(certificate -> certificate.isCurrentOn(referenceDate))
                .findFirst();
    }

    @Override
    public List<Certificate> findByAsset(AssetCode assetCode) {
        return certificates.values().stream().filter(certificate -> certificate.certifies(assetCode)).toList();
    }

    @Override
    public List<Certificate> findAll() {
        return List.copyOf(certificates.values());
    }
}
