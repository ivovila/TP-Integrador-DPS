package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record RenewCertificateCommand(CertificateNumber currentNumber, InspectionId inspectionId, Person renewedBy,
                                      RequestedRenewalScope requestedScope) implements LocksAsset {

    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.inspection(inspectionId);
    }
}
