package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.certification.model.exceptions.SubsystemNotFoundException;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.util.List;

public record InspectionResult(InspectionId inspectionId, AssetCode assetCode, InspectionClosure closure,
                               boolean coversWholeSchema, List<OpenNonConformity> openNonConformities,
                               SchemaReference schemaReference, List<SubsystemResult> subsystems,
                               CertificationScope scope) {

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
        return new InspectionResult(inspectionId, assetCode, closure, subsystemResult.covered(),
                subsystemResult.openNonConformities(), schemaReference, subsystems, narrowedScope);
    }

    public boolean requiresPartialCertificates() {
        return scope.requiresPartialCertificates(subsystems);
    }

    public boolean isClosed() {
        return closure.isClosed();
    }

    public InspectionReference reference() {
        return closure.referenceOf(inspectionId);
    }
}
