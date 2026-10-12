package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.CorrectiveActionId;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public interface PlanCorrectiveActionUseCase {

    CorrectiveActionId execute(FindingId findingId, String description, Person responsible, LocalDate dueDate,
                             Person plannedBy);
}
