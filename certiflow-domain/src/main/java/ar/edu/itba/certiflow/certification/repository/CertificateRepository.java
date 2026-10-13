package ar.edu.itba.certiflow.certification.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository {

    void save(Certificate certificate);

    Optional<Certificate> findByNumber(CertificateNumber number);

    /** El activo de un certificado, sin cargarlo: es lo que se bloquea antes de leerlo para cambiarlo. */
    Optional<AssetCode> assetCodeOf(CertificateNumber certificateNumber);

    List<Certificate> findByAsset(AssetCode assetCode);

    List<Certificate> findAll();
}
