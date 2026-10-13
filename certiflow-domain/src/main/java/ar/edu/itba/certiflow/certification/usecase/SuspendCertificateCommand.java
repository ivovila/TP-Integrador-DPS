package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record SuspendCertificateCommand(CertificateNumber number, String reason, Person suspendedBy) implements LocksAsset {
    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.certificate(number);
    }
}
