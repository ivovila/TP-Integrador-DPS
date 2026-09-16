package ar.edu.itba.certiflow.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Validity interval is [issuedAt, expiresAt). Expiration is computed, not scheduled. */
public final class Certificate {
    public enum Status {
        ACTIVE,
        SUSPENDED,
        EXPIRED,
        SUPERSEDED
    }

    private final UUID id, assetId, inspectionId;
    private final int inspectionRevision;
    private final Instant issuedAt, expiresAt;
    private final Status status;
    private final List<AuditEntry> history;

    private Certificate(
            UUID id,
            UUID assetId,
            UUID inspectionId,
            int revision,
            Instant issuedAt,
            Instant expiresAt,
            Status status,
            List<AuditEntry> history) {
        this.id = id;
        this.assetId = assetId;
        this.inspectionId = inspectionId;
        this.inspectionRevision = revision;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.status = status;
        this.history = List.copyOf(history);
    }

    public static Certificate issue(
            UUID id,
            Inspection inspection,
            List<CorrectiveAction> actions,
            String actor,
            Instant at) {
        Objects.requireNonNull(id);
        CertificationEligibility.requireEligible(inspection, actions);
        Checks.require(
                !at.isBefore(inspection.closedAt().orElseThrow()),
                "Cannot issue before inspection closure");
        return new Certificate(
                id,
                inspection.asset().id(),
                inspection.id(),
                inspection.revision(),
                at,
                at.plus(
                        inspection.version().orElseThrow().certificateValidityDays(),
                        ChronoUnit.DAYS),
                Status.ACTIVE,
                List.of(
                        new AuditEntry(
                                at,
                                actor,
                                "ISSUED",
                                "Inspection="
                                        + inspection.id()
                                        + "; revision="
                                        + inspection.revision())));
    }

    public Status statusAt(Instant at, Inspection current) {
        Checks.require(current.id().equals(inspectionId), "Wrong source inspection");
        Checks.require(!at.isBefore(issuedAt), "Cannot query certificate before issuance");
        if (status == Status.SUPERSEDED) return status;
        if (!at.isBefore(expiresAt)) return Status.EXPIRED;
        if (status == Status.SUSPENDED
                || current.revision() != inspectionRevision
                || current.state() != Inspection.State.CLOSED) return Status.SUSPENDED;
        return Status.ACTIVE;
    }

    public Certificate suspend(String reason, String actor, Instant at, Inspection current) {
        Checks.require(
                statusAt(at, current) == Status.ACTIVE,
                "Only active certificates can be suspended");
        return transition(Status.SUSPENDED, actor, at, "SUSPENDED", Checks.text(reason, "reason"));
    }

    public Certificate supersede(UUID replacement, String actor, Instant at) {
        Checks.require(status != Status.SUPERSEDED, "Certificate already renewed");
        return transition(
                Status.SUPERSEDED,
                actor,
                at,
                "RENEWED",
                "Replacement=" + Objects.requireNonNull(replacement));
    }

    private Certificate transition(
            Status next, String actor, Instant at, String operation, String detail) {
        Checks.require(
                !at.isBefore(history.get(history.size() - 1).at()),
                "Audit time cannot move backwards");
        return new Certificate(
                id,
                assetId,
                inspectionId,
                inspectionRevision,
                issuedAt,
                expiresAt,
                next,
                AuditEntry.append(history, new AuditEntry(at, actor, operation, detail)));
    }

    public UUID id() {
        return id;
    }

    public UUID assetId() {
        return assetId;
    }

    public UUID inspectionId() {
        return inspectionId;
    }

    public int inspectionRevision() {
        return inspectionRevision;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public List<AuditEntry> history() {
        return history;
    }
}
