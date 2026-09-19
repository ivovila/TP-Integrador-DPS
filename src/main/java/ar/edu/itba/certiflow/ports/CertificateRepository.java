package ar.edu.itba.certiflow.ports;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.certificate.Certificate;
import java.time.LocalDate;
import java.util.Optional;

public interface CertificateRepository {

    void save(Certificate certificate);

    Optional<Certificate> findCurrentFor(Asset asset, LocalDate referenceDate);
}
