package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.shared.model.Person;

public final class CertificateFactory {

    public Certificate create(CertificateNumber number, InspectionResult basedOn, CertificationDecision decision, Stamp issued) {
        return new Certificate(number, basedOn, decision, issued);
    }

    public Certificate createGlobal(CertificateNumber number, DecidedGlobalCertification decided, Person issuedBy) {
        return new Certificate(number, decided.assetCode(), decided.certificateBasis(), decided.scope(),
                decided.schemaReference(), decided.requiredSubsystems(), decided.decision(),
                new Stamp(issuedBy, decided.decidedAt()));
    }

    public Certificate restore(CertificateSnapshot snapshot) {
        return new Certificate(snapshot);
    }
}
