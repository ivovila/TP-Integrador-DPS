package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;

public record AddObservationCommand(InspectionId inspectionId, String criterionCode, String text, Person observedBy) {
}
