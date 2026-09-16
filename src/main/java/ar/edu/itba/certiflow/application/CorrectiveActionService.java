package ar.edu.itba.certiflow.application;

import ar.edu.itba.certiflow.application.port.ActionRepository;
import ar.edu.itba.certiflow.application.port.InspectionRepository;
import ar.edu.itba.certiflow.domain.Checks;
import ar.edu.itba.certiflow.domain.CorrectiveAction;
import ar.edu.itba.certiflow.domain.DomainException;
import ar.edu.itba.certiflow.domain.Evidence;
import ar.edu.itba.certiflow.domain.Inspection;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

public final class CorrectiveActionService {
    private final InspectionRepository inspections;
    private final ActionRepository actions;
    private final Clock clock;

    public CorrectiveActionService(
            InspectionRepository inspections, ActionRepository actions, Clock clock) {
        this.inspections = Objects.requireNonNull(inspections);
        this.actions = Objects.requireNonNull(actions);
        this.clock = Objects.requireNonNull(clock);
    }

    public CorrectiveAction plan(
            UUID inspectionId,
            UUID findingId,
            String description,
            String responsible,
            LocalDate due,
            String actor) {
        var inspection = inspection(inspectionId);
        Checks.require(inspection.state() == Inspection.State.CLOSED, "Inspection must be closed");
        var finding =
                inspection.findings().stream()
                        .filter(f -> f.id().equals(findingId))
                        .findFirst()
                        .orElseThrow(() -> new DomainException("Current finding not found"));
        Checks.require(
                actions.findByInspection(inspectionId).stream()
                        .noneMatch(a -> a.finding().id().equals(findingId)),
                "Finding already has a corrective action");
        var action =
                CorrectiveAction.plan(
                        UUID.randomUUID(),
                        finding,
                        description,
                        responsible,
                        due,
                        LocalDate.now(clock),
                        actor,
                        clock.instant());
        actions.save(action);
        return action;
    }

    public CorrectiveAction get(UUID id) {
        return actions.findById(id).orElseThrow(() -> new DomainException("Action not found"));
    }

    public CorrectiveAction submit(UUID id, List<Evidence> evidence, String actor) {
        return update(id, a -> a.submit(evidence, actor, clock.instant()));
    }

    public CorrectiveAction verify(UUID id, boolean accepted, String reason, String actor) {
        return update(id, a -> a.verify(accepted, reason, actor, clock.instant()));
    }

    public CorrectiveAction close(UUID id, String actor) {
        return update(id, a -> a.close(actor, clock.instant()));
    }

    private Inspection inspection(UUID id) {
        return inspections
                .findById(id)
                .orElseThrow(() -> new DomainException("Inspection not found"));
    }

    private CorrectiveAction update(UUID id, UnaryOperator<CorrectiveAction> operation) {
        var current = get(id);
        var inspection = inspection(current.finding().inspectionId());
        Checks.require(
                inspection.state() == Inspection.State.CLOSED
                        && inspection.revision() == current.finding().revision(),
                "Action belongs to a superseded inspection revision");
        var updated = operation.apply(current);
        actions.save(updated);
        return updated;
    }
}
