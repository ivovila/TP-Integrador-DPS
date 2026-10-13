package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record CorrectiveActionSnapshot(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate,
                                       Stamp planned, List<Verification> verifications,
                                       CancellationState cancellation) {

    public CorrectiveActionSnapshot {
        verifications = List.copyOf(verifications);
    }

    public Person plannedBy() {
        return planned.author();
    }

    public Instant plannedAt() {
        return planned.instant();
    }
}
