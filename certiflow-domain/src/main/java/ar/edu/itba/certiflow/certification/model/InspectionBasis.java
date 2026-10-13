package ar.edu.itba.certiflow.certification.model;

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
    public boolean backsGlobalCertificate() {
        return false;
    }

    @Override
    public <T> T describedBy(CertificateBasisDescriber<T> describer) {
        return describer.inspection(inspection);
    }
}
