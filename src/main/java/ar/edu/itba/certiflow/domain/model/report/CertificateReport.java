package ar.edu.itba.certiflow.domain.model.report;

import java.time.LocalDate;

import ar.edu.itba.certiflow.domain.model.asset.Asset;
import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.asset.AssetType;
import ar.edu.itba.certiflow.domain.model.asset.Location;
import ar.edu.itba.certiflow.domain.model.certificate.Certificate;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateId;
import ar.edu.itba.certiflow.domain.model.certificate.CertificateStatus;
import ar.edu.itba.certiflow.domain.model.certificate.ValidityPeriod;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;

public record CertificateReport(CertificateId certificate, AssetId asset, AssetType assetType, Location location,
                                PersonId responsible, InspectionId inspection, ValidityPeriod validity,
                                CertificateStatus status, LocalDate reportDate, boolean valid) {

    public static CertificateReport of(Certificate certificate, Asset asset, LocalDate date) {
        if (!certificate.getAsset().equals(asset.getId())) {
            throw new IllegalArgumentException("El activo no corresponde al certificado");
        }
        return new CertificateReport(certificate.getId(), asset.getId(), asset.getType(), asset.getLocation(),
                asset.getResponsible(), certificate.getInspection(), certificate.getValidity(),
                certificate.getStatus(), date, certificate.isValidOn(date));
    }
}
