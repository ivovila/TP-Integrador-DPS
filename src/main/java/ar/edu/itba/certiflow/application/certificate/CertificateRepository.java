package ar.edu.itba.certiflow.application.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.certificate.Certificate;
import java.time.LocalDate;
import java.util.Optional;

public interface CertificateRepository {

    void save(Certificate certificate);

    Optional<Certificate> findCurrentFor(Asset asset, LocalDate date);
}
