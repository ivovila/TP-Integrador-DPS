package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record InspectionResult(InspectionId inspectionId, AssetCode assetCode, Instant closingInstant,
                               boolean coversWholeSchema, List<OpenNonConformity> openNonConformities, SchemaReference schemaReference, List<SubsystemResult> subsystems, CertificationScope scope) {

    /** Cuándo se cerró la inspección, si ya se cerró. */
    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closingInstant);
    }


    public InspectionResult {
        openNonConformities = List.copyOf(openNonConformities);
        subsystems = List.copyOf(subsystems);
    }

    /**
     * Pedir el activo entero sobre una versión con subsistemas no es un error: la decisión lo rechaza
     * con {@link StandardIneligibilityReason#REQUIRES_PARTIAL_CERTIFICATES} (ver {@link #requiresPartialCertificates()}).
     */
    public InspectionResult forScope(CertificationScope requestedScope) {
        return requestedScope.narrow(this);
    }

    InspectionResult ofSubsystem(SubsystemCode subsystemCode, CertificationScope narrowedScope) {
        SubsystemResult subsystemResult = subsystems.stream()
                .filter(candidate -> candidate.code().equals(subsystemCode))
                .findFirst()
                .orElseThrow(() -> new SubsystemNotFoundException(subsystemCode));
        return new InspectionResult(inspectionId, assetCode, closingInstant, subsystemResult.covered(),
                subsystemResult.openNonConformities(), schemaReference, subsystems, narrowedScope);
    }

    public boolean requiresPartialCertificates() {
        return scope.requiresPartialCertificates(subsystems);
    }

    public boolean isClosed() {
        return closedAt().isPresent();
    }

    public InspectionReference reference() {
        return new InspectionReference(inspectionId, closedAt().orElseThrow(InspectionNotClosedException::new));
    }

}
