package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public final class CorrectiveAction {

    private final String description;
    private final Person responsible;
    private final LocalDate dueDate;
    private final List<Verification> verifications = new ArrayList<>();

    CorrectiveAction(String description, Person responsible, LocalDate dueDate) {
        this.description = description;
        this.responsible = responsible;
        this.dueDate = dueDate;
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

    public List<Verification> verifications() {
        return List.copyOf(verifications);
    }
}
