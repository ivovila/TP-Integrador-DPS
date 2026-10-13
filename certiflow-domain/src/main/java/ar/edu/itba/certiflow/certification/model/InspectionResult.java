package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record InspectionResult(InspectionId inspectionId, AssetCode assetCode, Optional<Instant> closedAt,
                               boolean coversWholeSchema, List<OpenNonConformity> openNonConformities, SchemaReference schemaReference, List<SubsystemResult> subsystems, CertificationScope scope) {

    public InspectionResult {
        openNonConformities = List.copyOf(openNonConformities);
        subsystems = List.copyOf(subsystems);
    }

    /**
     * Pedir el activo entero sobre una versión con subsistemas no es un error: la decisión lo rechaza
     * con {@link IneligibilityReason#REQUIRES_PARTIAL_CERTIFICATES} (ver {@link #requiresPartialCertificates()}).
     */
    public InspectionResult forScope(CertificationScope requested) {
        if (requested instanceof CertificationScope.WholeAsset) {
            return this;
        }
        var code = ((CertificationScope.SubsystemScope) requested).code();
        var subsystem = subsystems.stream().filter(item -> item.code().equals(code)).findFirst()
                .orElseThrow(() -> new SubsystemNotFoundException(code));
        return new InspectionResult(inspectionId, assetCode, closedAt, subsystem.covered(), subsystem.openNonConformities(),
                schemaReference, subsystems, requested);
    }

    public boolean requiresPartialCertificates() {
        return scope instanceof CertificationScope.WholeAsset && !subsystems.isEmpty();
    }

    public boolean isClosed() {
        return closedAt.isPresent();
    }

    public InspectionReference reference() {
        return new InspectionReference(inspectionId, closedAt.orElseThrow(InspectionNotClosedException::new));
    }

}
