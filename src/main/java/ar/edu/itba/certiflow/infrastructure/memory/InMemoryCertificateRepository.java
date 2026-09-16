package ar.edu.itba.certiflow.infrastructure.memory;

import ar.edu.itba.certiflow.application.port.CertificateRepository;
import ar.edu.itba.certiflow.domain.Certificate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Single-process adapter; values are immutable snapshots. */
public final class InMemoryCertificateRepository implements CertificateRepository {
    private final Map<UUID, Certificate> values = new LinkedHashMap<>();

    public Optional<Certificate> findById(UUID id) {
        return Optional.ofNullable(values.get(id));
    }

    public void save(Certificate value) {
        values.put(value.id(), value);
    }

    public List<Certificate> findByAsset(UUID id) {
        return values.values().stream().filter(c -> c.assetId().equals(id)).toList();
    }
}
