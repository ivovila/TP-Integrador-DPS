package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record InspectionResult(InspectionId inspectionId, AssetCode assetCode, Optional<Instant> closedAt,
                               boolean coversWholeSchema, List<OpenNonConformity> openNonConformities) {

    public InspectionResult {
        openNonConformities = List.copyOf(openNonConformities);
    }

    public boolean isClosed() {
        return closedAt.isPresent();
    }

    public InspectionReference reference() {
        return new InspectionReference(inspectionId, closedAt.orElseThrow(InspectionNotClosedException::new));
    }

    public List<OpenNonConformity> blockingNonConformities() {
        return openNonConformities.stream().filter(OpenNonConformity::blocksCertification).toList();
    }
}
