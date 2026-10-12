package ar.edu.itba.certiflow.inspection.usecase;

import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.LocalDate;

public record PlanCorrectiveActionCommand(FindingId findingId, String description, Person responsible, LocalDate dueDate, Person plannedBy) {
}
