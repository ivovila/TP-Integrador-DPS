package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, InspectionResult basedOn, ValidityPeriod validity, Person issuedBy,
                              Instant issuedAt) {
        return new Certificate(number, basedOn, validity, issuedBy, issuedAt);
    }
}
