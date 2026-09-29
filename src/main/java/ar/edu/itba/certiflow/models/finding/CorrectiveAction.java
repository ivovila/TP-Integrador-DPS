package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.finding.exceptions.CorrectiveActionAlreadyClosedException;
import ar.edu.itba.certiflow.models.finding.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CorrectiveAction {

    private final String description;
    private final Person responsible;
    private final LocalDate dueDate;
    private final Person plannedBy;
    private final Instant plannedAt;
    private final List<Verification> verifications = new ArrayList<>();
    private Cancellation cancellation;

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

    Verification verify(Person verifier, VerificationResult verificationResult, String verificationNotes, Instant verifiedAt) {
        ensureOpen();
        if (verifier.equals(responsible)) {
            throw new VerifierMustDifferFromResponsibleException(verifier);
        }
        Verification verification = new Verification(verifier, verificationResult, verificationNotes, verifiedAt);
        verifications.add(verification);
        return verification;
    }

    void cancel(Person cancelledBy, Instant cancelledAt) {
        ensureOpen();
        cancellation = new Cancellation(cancelledBy, cancelledAt);
    }

    public boolean isClosed() {
        return verifications.stream().anyMatch(Verification::isAccepted);
    }

    public boolean isCancelled() {
        return cancellation != null;
    }

    public boolean isOpen() {
        return !isClosed() && !isCancelled();
    }

    public boolean isOverdueOn(LocalDate referenceDate) {
        return isOpen() && referenceDate.isAfter(dueDate);
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

    public Optional<Cancellation> cancellation() {
        return Optional.ofNullable(cancellation);
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new CorrectiveActionAlreadyClosedException();
        }
    }
}
