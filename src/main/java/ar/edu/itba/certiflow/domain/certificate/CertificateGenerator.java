package ar.edu.itba.certiflow.domain.certificate;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;

public interface CertificateGenerator {

    Certificate generate(CertificateNumber number, Asset asset, Inspection basedOn, ValidityPeriod validity, Person by,
                         Instant at);
}
