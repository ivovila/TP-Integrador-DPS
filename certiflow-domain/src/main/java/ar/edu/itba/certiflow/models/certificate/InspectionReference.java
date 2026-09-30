package ar.edu.itba.certiflow.models.certificate;

import ar.edu.itba.certiflow.models.inspection.InspectionId;
import java.time.Instant;

public record InspectionReference(InspectionId id, Instant closedAt) {
}
