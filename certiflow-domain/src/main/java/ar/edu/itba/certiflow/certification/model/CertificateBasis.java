package ar.edu.itba.certiflow.certification.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface CertificateBasis {

    Instant latestInspectionClosedAt();

    boolean isSupportedOn(LocalDate date, List<Certificate> assetCertificates);

    boolean backsGlobalCertificate();

    <T> T describedBy(CertificateBasisDescriber<T> describer);
}
