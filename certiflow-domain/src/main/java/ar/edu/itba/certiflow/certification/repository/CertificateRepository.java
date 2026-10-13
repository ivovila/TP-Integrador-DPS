package ar.edu.itba.certiflow.certification.repository;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.Certificate;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CertificateRepository {

    void save(Certificate certificate);

    Optional<Certificate> findByNumber(CertificateNumber number);

    default Optional<Certificate> findCurrentFor(AssetCode assetCode, LocalDate referenceDate) {
        return findCurrentFor(assetCode, new CertificationScope.WholeAsset(), referenceDate);
    }

    default Optional<Certificate> findCurrentFor(AssetCode assetCode,
            CertificationScope scope, LocalDate date) {
        return findByAsset(assetCode).stream().filter(certificate -> certificate.scope().equals(scope) && certificate.isCurrentOn(date)).findFirst();
    }

    List<Certificate> findByAsset(AssetCode assetCode);

    List<Certificate> findAll();
}
