package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

/** Qué se va a hacer para resolver un hallazgo, quién lo hace y para cuándo. */
public record ActionPlan(String description, Person responsible, LocalDate dueDate) {
}
