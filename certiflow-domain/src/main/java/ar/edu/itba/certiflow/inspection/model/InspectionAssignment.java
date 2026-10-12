package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public record InspectionAssignment(Person inspector, LocalDate plannedDate, InspectionScope scope) {
}
