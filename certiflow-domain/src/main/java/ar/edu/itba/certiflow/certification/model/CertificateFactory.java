package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionView;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, AssetCode assetCode, InspectionView basedOn, ValidityPeriod validity, Person issuedBy,
                              Instant issuedAt) {
        return new Certificate(number, assetCode, basedOn, validity, issuedBy, issuedAt);
    }
}
