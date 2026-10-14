package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.certification.model.exceptions.NotAGlobalCertificateException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record InspectionBasis(InspectionReference inspection) implements CertificateBasis {

    @Override
    public Instant latestInspectionClosedAt() {
        return inspection.closedAt();
    }

    @Override
    public boolean isSupportedOn(LocalDate date, List<Certificate> assetCertificates) {
        return true;
    }

    @Override
    public void ensureBacksGlobalCertificate(CertificateNumber certificateNumber) {
        throw new NotAGlobalCertificateException(certificateNumber);
    }

    @Override
    public <T> T describedBy(CertificateBasisDescriber<T> describer) {
        return describer.inspection(inspection);
    }
}
