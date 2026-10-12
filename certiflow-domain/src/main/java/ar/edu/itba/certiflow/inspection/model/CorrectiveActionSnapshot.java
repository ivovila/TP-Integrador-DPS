package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record CorrectiveActionSnapshot(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate,
                                       Person plannedBy, Instant plannedAt, List<Verification> verifications,
                                       Optional<Cancellation> cancellation) {

    public CorrectiveActionSnapshot {
        verifications = List.copyOf(verifications);
    }
}
