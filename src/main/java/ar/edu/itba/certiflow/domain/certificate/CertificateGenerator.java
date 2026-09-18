package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.InspectionRecord;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public interface CertificateGenerator {

    Certificate generate(CertificateNumber number, Asset asset, InspectionRecord basedOn, ValidityPeriod validity,
                         Person by, Instant at);
}
