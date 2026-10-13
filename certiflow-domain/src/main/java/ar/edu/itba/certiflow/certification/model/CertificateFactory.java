package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.Set;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, InspectionResult basedOn, CertificationDecision decision, Person issuedBy,
                              Instant issuedAt) {
        return new Certificate(number, basedOn, decision, issuedBy, issuedAt);
    }

    public Certificate createGlobal(CertificateNumber number, AssetCode assetCode,
            CertificateBasis.PartialCertificatesBasis basis, SchemaReference schemaReference,
            Set<SubsystemCode> requiredSubsystems,
            CertificationDecision decision, Person issuedBy, Instant issuedAt) {
        return new Certificate(number, assetCode, basis, new CertificationScope.WholeAsset(), schemaReference,
                requiredSubsystems, decision, issuedBy, issuedAt);
    }

    public Certificate restore(CertificateSnapshot snapshot) {
        return new Certificate(snapshot);
    }
}
