package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.Person;

public record CloseInspectionCommand(InspectionId inspectionId, Person closedBy) {
}
