package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.DueDateInThePastException;
import ar.edu.itba.certiflow.inspection.model.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.RequiredValueMissingException;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CorrectiveAction {

    private final CorrectiveActionId id;
    private final String description;
    private final Person responsible;
    private final LocalDate dueDate;
    private final Stamp planned;
    private final List<Verification> verifications = new ArrayList<>();
    private Cancellation cancellation;

    CorrectiveAction(CorrectiveActionId id, String description, Person responsible, LocalDate dueDate, Stamp planned,
                     LocalDate plannedOn) {
        this.id = id;
        this.description = description;
        this.responsible = responsible;
        this.dueDate = dueDate;
        this.planned = planned;
        if (description.isBlank()) {
            throw new RequiredValueMissingException("A corrective action must describe what will be done");
        }
        if (dueDate.isBefore(plannedOn)) {
            throw new DueDateInThePastException(dueDate, plannedOn);
        }
    }

    CorrectiveAction(CorrectiveActionSnapshot snapshot) {
        this.id = snapshot.id();
        this.description = snapshot.description();
        this.responsible = snapshot.responsible();
        this.dueDate = snapshot.dueDate();
        this.planned = snapshot.planned();
        this.verifications.addAll(snapshot.verifications());
        this.cancellation = snapshot.cancellation().orElse(null);
    }

    CorrectiveActionSnapshot snapshot() {
        return new CorrectiveActionSnapshot(id, description, responsible, dueDate, planned, verifications(), cancellation());
    }

    Verification verify(VerificationResult verificationResult, String verificationNotes, Stamp verified) {
        ensureOpen();
        if (verified.by().equals(responsible)) {
            throw new VerifierMustDifferFromResponsibleException(verified.by());
        }
        Verification verification = new Verification(verificationResult, verificationNotes, verified);
        verifications.add(verification);
        return verification;
    }

    void cancel(Stamp cancelled) {
        ensureOpen();
        cancellation = new Cancellation(cancelled);
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

    boolean isIdentifiedBy(CorrectiveActionId candidate) {
        return id.equals(candidate);
    }

    public CorrectiveActionId id() {
        return id;
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
        return planned.by();
    }

    public Instant plannedAt() {
        return planned.at();
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
