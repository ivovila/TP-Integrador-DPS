package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CorrectiveAction {

    private final String description;
    private final Person responsible;
    private final LocalDate dueDate;
    private final Person plannedBy;
    private final Instant plannedAt;
    private final List<Verification> verifications = new ArrayList<>();

    CorrectiveAction(String description, Person responsible, LocalDate dueDate, Person plannedBy, Instant plannedAt) {
        this.description = description;
        this.responsible = responsible;
        this.dueDate = dueDate;
        this.plannedBy = plannedBy;
        this.plannedAt = plannedAt;
        if (description.isBlank()) {
            throw new IllegalArgumentException("A corrective action must describe what will be done");
        }
    }

    Verification verify(Person verifier, VerificationResult result, String notes, Instant at) {
        if (isClosed()) {
            throw new CorrectiveActionAlreadyClosedException();
        }
        if (verifier.equals(responsible)) {
            throw new VerifierMustDifferFromResponsibleException(verifier);
        }
        Verification verification = new Verification(verifier, result, notes, at);
        verifications.add(verification);
        return verification;
    }

    public boolean isClosed() {
        return verifications.stream().anyMatch(Verification::isAccepted);
    }

    public boolean isOverdueOn(LocalDate date) {
        return !isClosed() && date.isAfter(dueDate);
    }

    public String description() {
        return description;
    }

    public Person responsible() {
        return responsible;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public Person plannedBy() {
        return plannedBy;
    }

    public Instant plannedAt() {
        return plannedAt;
    }

    public List<Verification> verifications() {
        return List.copyOf(verifications);
    }
}
