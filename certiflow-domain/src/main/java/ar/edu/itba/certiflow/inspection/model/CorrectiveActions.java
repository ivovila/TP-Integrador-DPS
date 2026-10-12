package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.inspection.model.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Las acciones correctivas de un hallazgo. La colección es dueña de la numeración dentro del hallazgo
 * y de lo que significa que el hallazgo siga abierto: ninguna acción aceptada todavía.
 */
final class CorrectiveActions {

    private final List<CorrectiveAction> actions = new ArrayList<>();

    CorrectiveActions() {
    }

    CorrectiveActions(List<CorrectiveActionSnapshot> snapshots) {
        snapshots.forEach(snapshot -> actions.add(new CorrectiveAction(snapshot)));
    }

    List<CorrectiveActionSnapshot> snapshot() {
        return actions.stream().map(CorrectiveAction::snapshot).toList();
    }

    CorrectiveAction plan(String description, Person responsible, LocalDate dueDate, Person plannedBy,
                          Instant plannedAt, LocalDate plannedOn) {
        CorrectiveAction plannedAction = new CorrectiveAction(new CorrectiveActionId(actions.size() + 1), description,
                responsible, dueDate, plannedBy, plannedAt, plannedOn);
        actions.add(plannedAction);
        return plannedAction;
    }

    CorrectiveAction withId(CorrectiveActionId actionId) {
        return actions.stream()
                .filter(action -> action.isIdentifiedBy(actionId))
                .findFirst()
                .orElseThrow(() -> new CorrectiveActionNotInFindingException(actionId));
    }

    /** Una acción aceptada cierra el hallazgo; las demás se cancelan para que ninguna quede huérfana. */
    void cancelOpen(Person cancelledBy, Instant cancelledAt) {
        actions.stream().filter(CorrectiveAction::isOpen).forEach(openAction -> openAction.cancel(cancelledBy, cancelledAt));
    }

    boolean noneAccepted() {
        return actions.stream().noneMatch(CorrectiveAction::isClosed);
    }

    List<CorrectiveAction> asList() {
        return List.copyOf(actions);
    }
}
