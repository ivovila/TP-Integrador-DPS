package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.AssetCode;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, AssetCode assetCode, InspectionView basedOn, ValidityPeriod validity, Person issuedBy,
                              Instant issuedAt) {
        return new Certificate(number, assetCode, basedOn, validity, issuedBy, issuedAt);
    }
}
