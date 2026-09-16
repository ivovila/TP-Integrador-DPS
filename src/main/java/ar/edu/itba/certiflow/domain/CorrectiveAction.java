package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class CorrectiveAction {
    public enum State {
        PLANNED,
        SUBMITTED,
        VERIFIED,
        CLOSED
    }

    private final UUID id;
    private final Finding finding;
    private final String plan;
    private final String responsible;
    private final LocalDate dueDate;
    private final State state;
    private final List<Evidence> evidence;
    private final List<AuditEntry> history;

    private CorrectiveAction(
            UUID id,
            Finding finding,
            String plan,
            String responsible,
            LocalDate dueDate,
            State state,
            List<Evidence> evidence,
            List<AuditEntry> history) {
        this.id = id;
        this.finding = finding;
        this.plan = plan;
        this.responsible = responsible;
        this.dueDate = dueDate;
        this.state = state;
        this.evidence = List.copyOf(evidence);
        this.history = List.copyOf(history);
    }

    public static CorrectiveAction plan(
            UUID id,
            Finding finding,
            String plan,
            String responsible,
            LocalDate dueDate,
            LocalDate today,
            String actor,
            Instant at) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(finding);
        Checks.text(plan, "plan");
        Checks.text(responsible, "responsible");
        Objects.requireNonNull(dueDate);
        Checks.require(!dueDate.isBefore(today), "Due date cannot be in the past");
        return new CorrectiveAction(
                id,
                finding,
                plan,
                responsible,
                dueDate,
                State.PLANNED,
                List.of(),
                List.of(
                        new AuditEntry(
                                at,
                                actor,
                                "PLANNED",
                                plan + "; responsible=" + responsible + "; due=" + dueDate)));
    }

    public CorrectiveAction submit(List<Evidence> proof, String actor, Instant at) {
        Checks.require(state == State.PLANNED, "Action must be planned");
        proof = List.copyOf(proof);
        Checks.require(!proof.isEmpty(), "Correction needs evidence");
        return transition(
                State.SUBMITTED, proof, actor, at, "SUBMITTED", "Correction evidence=" + proof);
    }

    public CorrectiveAction verify(boolean accepted, String reason, String actor, Instant at) {
        Checks.require(state == State.SUBMITTED, "Only submitted actions can be verified");
        Checks.text(reason, "reason");
        Checks.require(
                !responsible.equals(actor), "Responsible person cannot verify own correction");
        return transition(
                accepted ? State.VERIFIED : State.PLANNED,
                evidence,
                actor,
                at,
                accepted ? "VERIFIED" : "VERIFICATION_REJECTED",
                reason);
    }

    public CorrectiveAction close(String actor, Instant at) {
        Checks.require(state == State.VERIFIED, "Verification is required before closure");
        return transition(
                State.CLOSED, evidence, actor, at, "CLOSED", "Verified correction closed");
    }

    private CorrectiveAction transition(
            State next,
            List<Evidence> proof,
            String actor,
            Instant at,
            String operation,
            String detail) {
        Checks.require(
                !at.isBefore(history.get(history.size() - 1).at()),
                "Audit time cannot move backwards");
        return new CorrectiveAction(
                id,
                finding,
                plan,
                responsible,
                dueDate,
                next,
                proof,
                AuditEntry.append(history, new AuditEntry(at, actor, operation, detail)));
    }

    public boolean isOverdue(LocalDate today) {
        return state != State.CLOSED && today.isAfter(dueDate);
    }

    public UUID id() {
        return id;
    }

    public Finding finding() {
        return finding;
    }

    public String plan() {
        return plan;
    }

    public String responsible() {
        return responsible;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public State state() {
        return state;
    }

    public List<Evidence> evidence() {
        return evidence;
    }

    public List<AuditEntry> history() {
        return history;
    }
}
