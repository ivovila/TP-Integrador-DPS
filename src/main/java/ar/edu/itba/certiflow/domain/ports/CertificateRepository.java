package ar.edu.itba.certiflow.domain.ports;

import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;

public interface CertificateRepository {

    Optional<Certificate> findById(CertificateId id);

    default Certificate getById(CertificateId id) {
        return findById(id).orElseThrow(() -> new NotFoundException("el certificado", id.value()));
    }

    void save(Certificate certificate);

    List<Certificate> findByAsset(AssetId assetId);
}
