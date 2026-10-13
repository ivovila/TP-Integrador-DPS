package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.VerifierMustDifferFromResponsibleException;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CorrectiveAction {

    private final CorrectiveActionId id;
    private final ActionPlan plan;
    private final Stamp planned;
    private final List<Verification> verifications = new ArrayList<>();
    private CancellationState cancellation = new NotCancelled();

    CorrectiveAction(CorrectiveActionId id, ActionPlan plan, Stamp planned, LocalDate plannedOn) {
        plan.ensureDueOnOrAfter(plannedOn);
        this.id = id;
        this.plan = plan;
        this.planned = planned;
    }

    CorrectiveAction(CorrectiveActionSnapshot snapshot) {
        this.id = snapshot.id();
        this.plan = new ActionPlan(snapshot.description(), snapshot.responsible(), snapshot.dueDate());
        this.planned = snapshot.planned();
        this.verifications.addAll(snapshot.verifications());
        this.cancellation = snapshot.cancellation();
    }

    CorrectiveActionSnapshot snapshot() {
        return new CorrectiveActionSnapshot(id, plan.description(), plan.responsible(), plan.dueDate(), planned,
                verifications(), cancellation);
    }

    Verification verify(VerificationResult verificationResult, String verificationNotes, Stamp verified) {
        ensureOpen();
        if (verified.author().equals(plan.responsible())) {
            throw new VerifierMustDifferFromResponsibleException(verified.author());
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
        return cancellation.isCancelled();
    }

    public boolean isOpen() {
        return !isClosed() && !isCancelled();
    }

    public boolean isOverdueOn(LocalDate referenceDate) {
        return isOpen() && referenceDate.isAfter(plan.dueDate());
    }

    boolean isIdentifiedBy(CorrectiveActionId candidate) {
        return id.equals(candidate);
    }

    public CorrectiveActionId id() {
        return id;
    }

    public String description() {
        return plan.description();
    }

    public Person responsible() {
        return plan.responsible();
    }

    public LocalDate dueDate() {
        return plan.dueDate();
    }

    public Person plannedBy() {
        return planned.author();
    }

    public Instant plannedAt() {
        return planned.instant();
    }

    public List<Verification> verifications() {
        return List.copyOf(verifications);
    }

    public CancellationState cancellation() {
        return cancellation;
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new CorrectiveActionAlreadyClosedException();
        }
    }
}
