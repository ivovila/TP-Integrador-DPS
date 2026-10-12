package ar.edu.itba.certiflow.certification.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository {

    void save(Certificate certificate);

    Optional<Certificate> findByNumber(CertificateNumber number);

    Optional<Certificate> findCurrentFor(AssetCode assetCode, LocalDate referenceDate);

    List<Certificate> findByAsset(AssetCode assetCode);

    List<Certificate> findAll();
}
