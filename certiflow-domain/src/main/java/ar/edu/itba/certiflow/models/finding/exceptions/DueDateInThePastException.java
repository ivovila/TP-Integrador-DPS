package ar.edu.itba.certiflow.models.finding.exceptions;

import ar.edu.itba.certiflow.models.shared.DomainException;
import java.time.LocalDate;

public class DueDateInThePastException extends DomainException {

    public DueDateInThePastException(LocalDate dueDate, LocalDate plannedOn) {
        super("A corrective action planned on " + plannedOn + " cannot be due on " + dueDate);
    }
}
