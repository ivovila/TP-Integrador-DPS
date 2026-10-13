package ar.edu.itba.certiflow.certification.usecase;

import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.certification.model.CertificationScope;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.util.Optional;
import ar.edu.itba.certiflow.shared.usecase.AssetLockKey;
import ar.edu.itba.certiflow.shared.usecase.LocksAsset;

/**
 * La renovación conserva el alcance del certificado actual. Si el cliente indica uno, tiene que coincidir;
 * sin alcance indicado se usa el constructor de tres argumentos.
 */
public record RenewCertificateCommand(CertificateNumber currentNumber, InspectionId inspectionId, Person renewedBy,
                                      CertificationScope requestedScope) implements LocksAsset {

    public RenewCertificateCommand(CertificateNumber currentNumber, InspectionId inspectionId, Person renewedBy) {
        this(currentNumber, inspectionId, renewedBy, null);
    }

    public Optional<CertificationScope> requested() {
        return Optional.ofNullable(requestedScope);
    }


    @Override
    public AssetLockKey assetLockKey() {
        return AssetLockKey.inspection(inspectionId);
    }
}
