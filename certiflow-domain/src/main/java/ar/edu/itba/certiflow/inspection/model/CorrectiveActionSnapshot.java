package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record CorrectiveActionSnapshot(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate,
                                       Stamp planned, List<Verification> verifications,
                                       Cancellation recordedCancellation) {

    /** La cancelación de la acción, si se canceló. */
    public Optional<Cancellation> cancellation() {
        return Optional.ofNullable(recordedCancellation);
    }


    public CorrectiveActionSnapshot {
        verifications = List.copyOf(verifications);
    }

    public Person plannedBy() {
        return planned.by();
    }

    public Instant plannedAt() {
        return planned.at();
    }
}
