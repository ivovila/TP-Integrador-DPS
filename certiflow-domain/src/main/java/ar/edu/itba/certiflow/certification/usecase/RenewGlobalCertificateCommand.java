package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.List;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

public record RenewGlobalCertificateCommand(CertificateNumber currentNumber, AssetCode assetCode, int schemaVersion,
        List<CertificateNumber> partialCertificateNumbers, Person issuedBy) implements LocksAsset {
    public RenewGlobalCertificateCommand { partialCertificateNumbers = List.copyOf(partialCertificateNumbers); }


    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.asset(assetCode);
    }
}
