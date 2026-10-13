package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.List;
import java.util.Set;

public record CertificateSnapshot(CertificateNumber number, AssetCode assetCode, CertificateBasis basis, CertificationScope scope, SchemaReference schemaReference,
                                  Set<SubsystemCode> requiredSubsystems,
                                  ValidityPeriod validity, Stamp issued, List<Suspension> suspensions,
                                  CertificateLifecycle lifecycle, CertificationDecision decision) {

    public CertificateSnapshot {
        suspensions = List.copyOf(suspensions);
        requiredSubsystems = Set.copyOf(requiredSubsystems);
    }

    public Person issuedBy() {
        return issued.by();
    }

    public Instant issuedAt() {
        return issued.at();
    }
}
