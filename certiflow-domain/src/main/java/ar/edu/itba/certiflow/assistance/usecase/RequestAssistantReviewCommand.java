package ar.edu.itba.certiflow.assistance.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;

public record RequestAssistantReviewCommand(InspectionId inspectionId, Person requestedBy) {
}