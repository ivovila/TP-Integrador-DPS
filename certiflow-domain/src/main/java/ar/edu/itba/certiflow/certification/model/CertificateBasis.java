package ar.edu.itba.certiflow.certification.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface CertificateBasis {

    Instant latestInspectionClosedAt();

    boolean isSupportedOn(LocalDate date, List<Certificate> assetCertificates);

    /** Solo un certificado respaldado por parciales es un global: el que se apoya en una inspección no lo es. */
    void ensureBacksGlobalCertificate(CertificateNumber certificateNumber);

    <T> T describedBy(CertificateBasisDescriber<T> describer);
}
