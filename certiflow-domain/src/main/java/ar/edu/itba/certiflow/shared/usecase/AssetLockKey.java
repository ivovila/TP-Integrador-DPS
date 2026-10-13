package ar.edu.itba.certiflow.shared.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;

/** Cómo llegar al activo que hay que bloquear: por su código o por algo que le pertenece. */
public record AssetLockKey(String owner, String id) {

    public static AssetLockKey asset(AssetCode code) {
        return new AssetLockKey("asset", code.value());
    }

    public static AssetLockKey inspection(InspectionId inspection) {
        return new AssetLockKey("inspection", inspection.value());
    }

    public static AssetLockKey certificate(CertificateNumber number) {
        return new AssetLockKey("certificate", number.value());
    }

    public static AssetLockKey finding(FindingId finding) {
        return new AssetLockKey("finding", finding.value());
    }
}
