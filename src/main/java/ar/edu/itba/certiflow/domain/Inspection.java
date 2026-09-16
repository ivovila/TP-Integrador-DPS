package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable aggregate: transitions return a new value, preserving previous snapshots. */
public final class Inspection {
    public enum State {
        ASSIGNED,
        IN_PROGRESS,
        CLOSED
    }

    private final UUID id;
    private final Asset asset;
    private final UUID schemeId;
    private final String inspector;
    private final LocalDate scheduledDate;
    private final String scope;
    private final SchemeVersion version;
    private final State state;
    private final int revision;
    private final Map<String, Submission> submissions;
    private final Map<String, Evaluation> evaluations;
    private final List<Finding> findings;
    private final List<InspectionRevision> previousRevisions;
    private final List<AuditEntry> history;
    private final Instant startedAt;
    private final Instant closedAt;

    private Inspection(
            UUID id,
            Asset asset,
            UUID schemeId,
            String inspector,
            LocalDate scheduledDate,
            String scope,
            SchemeVersion version,
            State state,
            int revision,
            Map<String, Submission> submissions,
            Map<String, Evaluation> evaluations,
            List<Finding> findings,
            List<InspectionRevision> previousRevisions,
            List<AuditEntry> history,
            Instant startedAt,
            Instant closedAt) {
        this.id = id;
        this.asset = asset;
        this.schemeId = schemeId;
        this.inspector = inspector;
        this.scheduledDate = scheduledDate;
        this.scope = scope;
        this.version = version;
        this.state = state;
        this.revision = revision;
        this.submissions = Map.copyOf(submissions);
        this.evaluations = Map.copyOf(evaluations);
        this.findings = List.copyOf(findings);
        this.previousRevisions = List.copyOf(previousRevisions);
        this.history = List.copyOf(history);
        this.startedAt = startedAt;
        this.closedAt = closedAt;
    }

    public static Inspection assign(
            UUID id,
            Asset asset,
            UUID schemeId,
            String inspector,
            LocalDate date,
            String scope,
            String actor,
            Instant at) {
        Objects.requireNonNull(id);
        Objects.requireNonNull(asset);
        Objects.requireNonNull(schemeId);
        Objects.requireNonNull(date);
        Checks.text(inspector, "inspector");
        Checks.text(scope, "scope");
        return new Inspection(
                id,
                asset,
                schemeId,
                inspector,
                date,
                scope,
                null,
                State.ASSIGNED,
                1,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(
                        new AuditEntry(
                                at,
                                actor,
                                "ASSIGNED",
                                "Inspector: "
                                        + inspector
                                        + "; scheduled: "
                                        + date
                                        + "; scope: "
                                        + scope)),
                null,
                null);
    }

    public Inspection start(SchemeVersion selected, String actor, Instant at) {
        Checks.require(state == State.ASSIGNED, "Only assigned inspections can start");
        Objects.requireNonNull(selected);
        Checks.require(
                selected.schemeId().equals(schemeId) && selected.assetType().equals(asset.type()),
                "Scheme does not apply to asset");
        Checks.require(!selected.publishedAt().isAfter(at), "Cannot use a future version");
        return new Inspection(
                id,
                asset,
                schemeId,
                inspector,
                scheduledDate,
                scope,
                selected,
                State.IN_PROGRESS,
                revision,
                submissions,
                evaluations,
                findings,
                previousRevisions,
                audit(actor, at, "STARTED", "Version " + selected.number()),
                at,
                null);
    }

    public Inspection record(
            String criterionCode, Submission submission, String actor, Instant at) {
        requireInProgress();
        Objects.requireNonNull(submission);
        version.criterion(criterionCode)
                .evaluate(submission); // Validate answer type and unit, allowing partial evidence.
        var updated = new LinkedHashMap<>(submissions);
        var before = updated.put(criterionCode, submission);
        return copy(
                State.IN_PROGRESS,
                revision,
                updated,
                Map.of(),
                List.of(),
                previousRevisions,
                audit(
                        actor,
                        at,
                        "ANSWER_RECORDED",
                        criterionCode + "; before=" + before + "; after=" + submission),
                null);
    }

    public Inspection evaluate(String actor, Instant at) {
        requireInProgress();
        var results = new LinkedHashMap<String, Evaluation>();
        version.criteria()
                .forEach(c -> results.put(c.code(), c.evaluate(submissions.get(c.code()))));
        return copy(
                state,
                revision,
                submissions,
                results,
                List.of(),
                previousRevisions,
                audit(actor, at, "EVALUATED", results.toString()),
                null);
    }

    public Inspection close(String actor, Instant at) {
        Inspection evaluated = evaluate(actor, at);
        Checks.require(
                evaluated.evaluations.values().stream()
                        .noneMatch(e -> e.outcome() == Outcome.INCOMPLETE),
                "Cannot close with incomplete criteria");
        var generated =
                version.criteria().stream()
                        .filter(c -> evaluated.evaluations.get(c.code()).isFinding())
                        .map(
                                c ->
                                        new Finding(
                                                UUID.randomUUID(),
                                                id,
                                                revision,
                                                c.code(),
                                                c.severity(),
                                                evaluated.evaluations.get(c.code()).explanation(),
                                                submissions.get(c.code()).evidence(),
                                                asset.responsible()))
                        .toList();
        return evaluated.copy(
                State.CLOSED,
                revision,
                submissions,
                evaluated.evaluations,
                generated,
                previousRevisions,
                evaluated.audit(
                        actor,
                        at,
                        "CLOSED",
                        "Revision " + revision + "; findings=" + generated.size()),
                at);
    }

    public Inspection rectify(String reason, String actor, Instant at) {
        Checks.require(state == State.CLOSED, "Only closed inspections can be rectified");
        Checks.text(reason, "reason");
        var revisions = new ArrayList<>(previousRevisions);
        revisions.add(
                new InspectionRevision(revision, submissions, evaluations, findings, closedAt));
        return copy(
                State.IN_PROGRESS,
                revision + 1,
                submissions,
                Map.of(),
                List.of(),
                revisions,
                audit(
                        actor,
                        at,
                        "RECTIFICATION_OPENED",
                        "Revision " + revision + " preserved; reason=" + reason),
                null);
    }

    private void requireInProgress() {
        Checks.require(state == State.IN_PROGRESS, "Inspection must be in progress");
    }

    private List<AuditEntry> audit(String actor, Instant at, String operation, String detail) {
        Checks.require(
                !at.isBefore(history.get(history.size() - 1).at()),
                "Audit time cannot move backwards");
        return AuditEntry.append(history, new AuditEntry(at, actor, operation, detail));
    }

    private Inspection copy(
            State next,
            int rev,
            Map<String, Submission> answers,
            Map<String, Evaluation> results,
            List<Finding> fs,
            List<InspectionRevision> revisions,
            List<AuditEntry> audit,
            Instant closed) {
        return new Inspection(
                id,
                asset,
                schemeId,
                inspector,
                scheduledDate,
                scope,
                version,
                next,
                rev,
                answers,
                results,
                fs,
                revisions,
                audit,
                startedAt,
                closed);
    }

    public UUID id() {
        return id;
    }

    public Asset asset() {
        return asset;
    }

    public UUID schemeId() {
        return schemeId;
    }

    public String inspector() {
        return inspector;
    }

    public LocalDate scheduledDate() {
        return scheduledDate;
    }

    public String scope() {
        return scope;
    }

    public Optional<SchemeVersion> version() {
        return Optional.ofNullable(version);
    }

    public State state() {
        return state;
    }

    public int revision() {
        return revision;
    }

    public Map<String, Submission> submissions() {
        return submissions;
    }

    public Map<String, Evaluation> evaluations() {
        return evaluations;
    }

    public List<Finding> findings() {
        return findings;
    }

    public List<InspectionRevision> previousRevisions() {
        return previousRevisions;
    }

    public List<AuditEntry> history() {
        return history;
    }

    public Optional<Instant> startedAt() {
        return Optional.ofNullable(startedAt);
    }

    public Optional<Instant> closedAt() {
        return Optional.ofNullable(closedAt);
    }
}
