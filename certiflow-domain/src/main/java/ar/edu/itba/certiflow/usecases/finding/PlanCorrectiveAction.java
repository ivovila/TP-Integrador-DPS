package ar.edu.itba.certiflow.usecases.finding;

import ar.edu.itba.certiflow.models.finding.CorrectiveActionId;
import ar.edu.itba.certiflow.models.finding.FindingId;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.LocalDate;

public interface PlanCorrectiveAction {

    CorrectiveActionId execute(FindingId findingId, String description, Person responsible, LocalDate dueDate,
                             Person plannedBy);
}
