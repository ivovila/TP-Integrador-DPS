package ar.edu.itba.certiflow.support;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.ports.CertificateRepository;

public class InMemoryCertificateRepository implements CertificateRepository {

    private final Map<CertificateId, Certificate> certificates = new LinkedHashMap<>();

    @Override
    public Optional<Certificate> findById(CertificateId id) {
        return Optional.ofNullable(certificates.get(id));
    }

    @Override
    public void save(Certificate certificate) {
        certificates.put(certificate.getId(), certificate);
    }

    @Override
    public List<Certificate> findByAsset(AssetId assetId) {
        return certificates.values().stream()
                .filter(certificate -> certificate.getAsset().equals(assetId))
                .toList();
    }
}
