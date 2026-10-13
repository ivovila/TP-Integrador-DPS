package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record IssueCertificateCommand(InspectionId inspectionId, Person issuedBy, CertificationScope scope) implements LocksAsset {
    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.inspection(inspectionId);
    }
}
