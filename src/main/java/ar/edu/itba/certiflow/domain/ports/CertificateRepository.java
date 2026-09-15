package ar.edu.itba.certiflow.domain.ports;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;

public interface CertificateRepository {

    Optional<Certificate> findById(CertificateId id);

    void save(Certificate certificate);

    List<Certificate> findByAsset(AssetId assetId);
}
