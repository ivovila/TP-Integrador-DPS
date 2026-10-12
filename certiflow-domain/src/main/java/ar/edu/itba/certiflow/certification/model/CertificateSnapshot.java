package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;

public record CertificateSnapshot(CertificateNumber number, AssetCode assetCode, InspectionReference basedOn,
                                  ValidityPeriod validity, Person issuedBy, Instant issuedAt, List<Suspension> suspensions,
                                  CertificateLifecycle lifecycle) {

    public CertificateSnapshot {
        suspensions = List.copyOf(suspensions);
    }
}
