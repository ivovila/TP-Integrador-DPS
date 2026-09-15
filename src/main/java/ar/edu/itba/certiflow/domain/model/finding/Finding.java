package ar.edu.itba.certiflow.domain.model.finding;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ar.edu.itba.certiflow.domain.model.asset.AssetId;
import ar.edu.itba.certiflow.domain.model.inspection.CriterionResult;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionEvaluation;
import ar.edu.itba.certiflow.domain.model.inspection.InspectionId;
import ar.edu.itba.certiflow.domain.model.schema.CriterionId;
import ar.edu.itba.certiflow.domain.model.shared.DomainEvent;
import ar.edu.itba.certiflow.domain.model.shared.DomainException;
import ar.edu.itba.certiflow.domain.model.shared.EventSource;
import ar.edu.itba.certiflow.domain.model.shared.Evidence;
import ar.edu.itba.certiflow.domain.model.shared.InvalidTransitionException;
import ar.edu.itba.certiflow.domain.model.shared.NotFoundException;
import ar.edu.itba.certiflow.domain.model.shared.PendingEvents;
import ar.edu.itba.certiflow.domain.model.shared.PersonId;
import ar.edu.itba.certiflow.domain.rules.CriterionOutcome;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Finding implements EventSource {

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
    @Getter(AccessLevel.NONE)
    private final PendingEvents events = new PendingEvents();

    private Finding(FindingId id, InspectionEvaluation evaluation, FindingDetails details,
                    List<Evidence> evidences, LocalDateTime raisedAt) {
        this.id = id;
        this.inspection = evaluation.inspection();
        this.asset = evaluation.asset();
        this.criterion = details.criterion();
        this.severity = details.severity();
        this.description = details.description();
        this.evidences = List.copyOf(evidences);
        this.responsible = details.responsible();
        this.raisedAt = raisedAt;
    }

    public static Finding raise(FindingId id, InspectionEvaluation evaluation, FindingDetails details,
                                List<Finding> inspectionFindings, LocalDateTime now) {
        CriterionResult result = evaluation.resultOf(details.criterion());
        if (result.outcome() == CriterionOutcome.APPROVED) {
            throw new DomainException("No se puede levantar un hallazgo sobre un criterio aprobado");
        }
        if (inspectionFindings.stream().anyMatch(finding -> !finding.getInspection().equals(evaluation.inspection()))) {
            throw new IllegalArgumentException("Los hallazgos recibidos no son de esta inspeccion");
        }
        if (inspectionFindings.stream().anyMatch(finding -> finding.getCriterion().equals(details.criterion()))) {
            throw new DomainException("Ya existe un hallazgo para ese criterio en la inspeccion");
        }
        Finding finding = new Finding(id, evaluation, details, result.evidences(), now);
        finding.events.add(new FindingRaised(id, evaluation.inspection(), details.severity(), now));
        return finding;
    }

    public CorrectiveAction planAction(CorrectiveActionId actionId, String actionDescription, PersonId assignee,
                                       LocalDate dueDate, LocalDateTime now) {
        checkOpen();
        if (dueDate.isBefore(now.toLocalDate())) {
            throw new DomainException("El vencimiento no puede ser anterior a hoy");
        }
        CorrectiveAction action = new CorrectiveAction(actionId, actionDescription, assignee, dueDate);
        actions.add(action);
        events.add(new CorrectiveActionPlanned(id, actionId, dueDate, now));
        return action;
    }

    public void verifyAction(CorrectiveActionId actionId, PersonId verifier, LocalDateTime now) {
        checkOpen();
        CorrectiveAction action = actions.stream()
                .filter(a -> a.getId().equals(actionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("la accion correctiva", actionId.value()));
        action.verify(verifier, now);
        events.add(new CorrectiveActionVerified(id, actionId, verifier, now));
    }

    public void close(LocalDateTime now) {
        checkOpen();
        if (actions.isEmpty() || !actions.stream().allMatch(CorrectiveAction::isVerified)) {
            throw new DomainException("El hallazgo se cierra con todas sus acciones correctivas verificadas");
        }
        closure = new Closure(now);
        events.add(new FindingClosed(id, now));
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

    @Override
    public List<DomainEvent> pullEvents() {
        return events.pull();
    }
}
