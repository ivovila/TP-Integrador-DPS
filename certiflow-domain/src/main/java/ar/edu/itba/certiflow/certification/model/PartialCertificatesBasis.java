package ar.edu.itba.certiflow.certification.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PartialCertificatesBasis(List<CertificateNumber> partialCertificateNumbers, PolicyCode globalPolicyCode,
                                       Instant latestInspectionClosedAt) implements CertificateBasis {

    public PartialCertificatesBasis {
        partialCertificateNumbers = List.copyOf(partialCertificateNumbers);
    }

    /** Cada parcial que lo respalda sigue activo entre los certificados del activo. */
    @Override
    public boolean isSupportedOn(LocalDate date, List<Certificate> assetCertificates) {
        return partialCertificateNumbers.stream().allMatch(partialCertificateNumber -> assetCertificates.stream()
                .anyMatch(candidate -> candidate.hasNumber(partialCertificateNumber)
                        && candidate.statusOn(date, assetCertificates).isActive()));
    }

    @Override
    public boolean backsGlobalCertificate() {
        return true;
    }

    @Override
    public <T> T describedBy(CertificateBasisDescriber<T> describer) {
        return describer.partialCertificates(partialCertificateNumbers, globalPolicyCode, latestInspectionClosedAt);
    }
}
