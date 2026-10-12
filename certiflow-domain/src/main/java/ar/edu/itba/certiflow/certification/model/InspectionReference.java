package ar.edu.itba.certiflow.certification.model;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import java.time.Instant;

public record InspectionReference(InspectionId id, Instant closedAt) {
}
