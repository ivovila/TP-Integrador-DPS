package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.inspection.Inspection;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.AggregateRoot;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import lombok.Getter;

@Getter
public class Finding extends AggregateRoot {

    private final FindingId id;
    private final InspectionId inspection;
    private final AssetId asset;
    private final CriterionId criterion;
    private final Severity severity;
    private final String description;
    private final List<Evidence> evidences;
    private final PersonId responsible;
    private final LocalDateTime raisedAt;
    private Closure closure;
    private final List<CorrectiveAction> actions = new ArrayList<>();

    private Finding(FindingId id, InspectionId inspection, AssetId asset, CriterionId criterion, Severity severity,
                    String description, List<Evidence> evidences, PersonId responsible, LocalDateTime raisedAt) {
        this.id = id;
        this.inspection = inspection;
        this.asset = asset;
        this.criterion = criterion;
        this.severity = severity;
        this.description = description;
        this.evidences = List.copyOf(evidences);
        this.responsible = responsible;
        this.raisedAt = raisedAt;
    }

    public static Finding raise(FindingId id, Inspection inspection, CriterionId criterion, Severity severity,
                                String description, PersonId responsible, LocalDateTime now) {
        if (inspection.evaluate().outcomeOf(criterion) == CriterionOutcome.APPROVED) {
            throw new DomainException("No se puede levantar un hallazgo sobre un criterio aprobado");
        }
        List<Evidence> evidences = inspection.responseFor(criterion).evidences();
        Finding finding = new Finding(id, inspection.getId(), inspection.getAsset(), criterion, severity,
                description, evidences, responsible, now);
        finding.recordEvent(new FindingRaised(id, inspection.getId(), severity, now));
        return finding;
    }

    public CorrectiveAction planAction(CorrectiveActionId actionId, String actionDescription, PersonId assignee,
                                       LocalDate dueDate, LocalDateTime now) {
        checkOpen();
        if (dueDate.isBefore(now.toLocalDate())) {
            throw new IllegalArgumentException("El vencimiento no puede ser anterior a hoy");
        }
        CorrectiveAction action = new CorrectiveAction(actionId, actionDescription, assignee, dueDate);
        actions.add(action);
        recordEvent(new CorrectiveActionPlanned(id, actionId, dueDate, now));
        return action;
    }

    public void verifyAction(CorrectiveActionId actionId, PersonId verifier, LocalDateTime now) {
        checkOpen();
        CorrectiveAction action = actions.stream()
                .filter(a -> a.getId().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("la accion correctiva", actionId.value()));
        action.verify(verifier, now);
        recordEvent(new CorrectiveActionVerified(id, actionId, verifier, now));
    }

    public void close(LocalDateTime now) {
        checkOpen();
        if (actions.isEmpty() || !actions.stream().allMatch(CorrectiveAction::isVerified)) {
            throw new DomainException("El hallazgo se cierra con todas sus acciones correctivas verificadas");
        }
        closure = new Closure(now);
        recordEvent(new FindingClosed(id, now));
    }

    public boolean isOpen() {
        return closure == null;
    }

    public Optional<Closure> getClosure() {
        return Optional.ofNullable(closure);
    }

    public boolean hasOverdueActions(LocalDate today) {
        return actions.stream().anyMatch(action -> action.isOverdue(today));
    }

    public List<CorrectiveAction> getActions() {
        return List.copyOf(actions);
    }

    private void checkOpen() {
        if (!isOpen()) {
            throw new InvalidTransitionException("El hallazgo esta cerrado");
        }
    }
}
