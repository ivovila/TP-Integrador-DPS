package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, Asset asset, InspectionView basedOn, ValidityPeriod validity, Person issuedBy,
                              Instant issuedAt) {
        return new Certificate(number, asset, basedOn, validity, issuedBy, issuedAt);
    }
}
