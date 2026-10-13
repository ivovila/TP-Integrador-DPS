package ar.edu.itba.certiflow.inspection.model.exceptions;

import ar.edu.itba.certiflow.shared.model.RuleViolationException;
import java.time.LocalDate;

public class DueDateInThePastException extends RuleViolationException {

    public DueDateInThePastException(LocalDate dueDate, LocalDate plannedOn) {
        super("A corrective action planned on " + plannedOn + " cannot be due on " + dueDate);
    }
}
