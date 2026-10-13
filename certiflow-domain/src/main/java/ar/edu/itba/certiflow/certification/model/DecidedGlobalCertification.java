package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Los respaldos de un global ya validados por la política global, con la decisión que resultó de ellos.
 * Emitir y renovar un global parten de esto; el global guarda los números, no los objetos, porque sus
 * respaldos son referencias inmutables y su estado se deriva al consultarlos.
 */
public record DecidedGlobalCertification(AssetCode assetCode, SchemaReference schemaReference,
                                         Set<SubsystemCode> requiredSubsystems, List<CertificateNumber> partialNumbers,
                                         String globalPolicyCode, Instant latestInspectionClosedAt,
                                         GrantedDecision decision) {

    public DecidedGlobalCertification {
        requiredSubsystems = Set.copyOf(requiredSubsystems);
        partialNumbers = List.copyOf(partialNumbers);
    }

    public CertificationScope scope() {
        return decision.scope();
    }

    public LocalDate validFrom() {
        return decision.validity().validFrom();
    }

    public Instant decidedAt() {
        return decision.evaluatedAt();
    }

    public CertificateBasis.PartialCertificatesBasis certificateBasis() {
        return new CertificateBasis.PartialCertificatesBasis(partialNumbers, globalPolicyCode, latestInspectionClosedAt);
    }
}
