package ar.edu.itba.certiflow.application.port;

import ar.edu.itba.certiflow.domain.Certificate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** save replaces the snapshot identified by id; missing lookups return Optional.empty(). */
public interface CertificateRepository {
    Optional<Certificate> findById(UUID id);

    void save(Certificate value);

    List<Certificate> findByAsset(UUID assetId);
}
