package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.schema.model.CriterionCode;
import ar.edu.itba.certiflow.shared.model.Person;

public record AddObservationCommand(InspectionId inspectionId, CriterionCode criterionCode, String text, Person observedBy) {
}
