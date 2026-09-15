package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import lombok.Getter;

@Getter
public class CorrectiveAction {

    private final CorrectiveActionId id;
    private final String description;
    private final PersonId assignee;
    private final LocalDate dueDate;
    private Verification verification;

    CorrectiveAction(CorrectiveActionId id, String description, PersonId assignee, LocalDate dueDate) {
        this.id = id;
        this.description = description;
        this.assignee = assignee;
        this.dueDate = dueDate;
    }

    void verify(PersonId verifiedBy, LocalDateTime now) {
        if (isVerified()) {
            throw new InvalidTransitionException("La accion correctiva ya fue verificada");
        }
        if (verifiedBy.equals(assignee)) {
            throw new DomainException("Quien ejecuta la accion correctiva no puede verificarla");
        }
        verification = new Verification(verifiedBy, now);
    }

    public Optional<Verification> getVerification() {
        return Optional.ofNullable(verification);
    }

    public boolean isVerified() {
        return verification != null;
    }

    public boolean isOverdue(LocalDate today) {
        return !isVerified() && today.isAfter(dueDate);
    }
}
