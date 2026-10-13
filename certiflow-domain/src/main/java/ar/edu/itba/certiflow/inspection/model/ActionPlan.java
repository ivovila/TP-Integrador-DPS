package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.DueDateInThePastException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import java.time.LocalDate;

/** Qué se va a hacer para resolver un hallazgo, quién lo hace y para cuándo. */
public record ActionPlan(String description, Person responsible, LocalDate dueDate) {

    public ActionPlan {
        if (description.isBlank()) {
            throw new RequiredValueMissingException("A corrective action must describe what will be done");
        }
    }

    void ensureDueOnOrAfter(LocalDate plannedOn) {
        if (dueDate.isBefore(plannedOn)) {
            throw new DueDateInThePastException(dueDate, plannedOn);
        }
    }
}
