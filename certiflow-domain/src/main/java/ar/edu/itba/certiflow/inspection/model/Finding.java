package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingRequiresNonConformityException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Finding {

    private final FindingId id;
    private final InspectionId inspectionId;
    private final AssetCode assetCode;
    private final Criterion<?> criterion;
    private final List<AttachedEvidence> evidence;
    private final Person responsible;
    private final CorrectiveActions actions;
    private final AuditTrail auditTrail = new AuditTrail();

    Finding(FindingId id, InspectionId inspectionId, AssetCode assetCode, Person responsible, CriterionResponse<?> nonConformity,
            Person raisedBy, Instant raisedAt) {
        this.id = id;
        this.inspectionId = inspectionId;
        this.assetCode = assetCode;
        this.responsible = responsible;
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.attachments();
        this.actions = new CorrectiveActions();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
        auditTrail.record(FindingAudit.RAISED, raisedBy, raisedAt, criterion.code() + " " + nonConformity.outcome());
    }

    Finding(FindingSnapshot snapshot) {
        this.id = snapshot.id();
        this.inspectionId = snapshot.inspectionId();
        this.assetCode = snapshot.assetCode();
        this.criterion = snapshot.criterion();
        this.evidence = snapshot.evidence();
        this.responsible = snapshot.responsible();
        this.actions = new CorrectiveActions(snapshot.actions());
    }

    public FindingSnapshot snapshot() {
        return new FindingSnapshot(id, inspectionId, assetCode, criterion, evidence, responsible, actions.snapshot());
    }

    CorrectiveAction planAction(String description, Person actionResponsible, LocalDate dueDate, Person plannedBy,
                                       Instant plannedAt, LocalDate plannedOn) {
        ensureOpen();
        CorrectiveAction plannedAction = actions.plan(description, actionResponsible, dueDate, plannedBy, plannedAt, plannedOn);
        auditTrail.record(FindingAudit.ACTION_PLANNED, plannedBy, plannedAt, description + ", due " + dueDate);
        return plannedAction;
    }

    Verification verifyAction(CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                                     String verificationNotes, Instant verifiedAt) {
        ensureOpen();
        CorrectiveAction correctiveAction = actions.withId(actionId);
        Verification verification = correctiveAction.verify(verifier, verificationResult, verificationNotes, verifiedAt);
        if (verification.isAccepted()) {
            actions.cancelOpen(verifier, verifiedAt);
        }
        auditTrail.record(verificationResult.audit(), verifier, verifiedAt, verificationNotes);
        return verification;
    }

    public boolean isOpen() {
        return actions.noneAccepted();
    }

    public boolean blocksCertification() {
        return isOpen() && severity().blocksCertification();
    }

    public boolean concerns(AssetCode candidate) {
        return assetCode.equals(candidate);
    }

    /** Un hallazgo abierto por esa misma no conformidad: evita duplicarlo al rectificar. */
    public boolean isOpenFor(InspectionId sourceInspection, CriterionResponse<?> nonConformity) {
        return isOpen() && inspectionId.equals(sourceInspection) && nonConformity.isFor(criterion);
    }

    public FindingId id() {
        return id;
    }

    public AssetCode assetCode() {
        return assetCode;
    }

    public InspectionId inspectionId() {
        return inspectionId;
    }

    public Criterion<?> criterion() {
        return criterion;
    }

    public Severity severity() {
        return criterion.severity();
    }

    public List<AttachedEvidence> evidence() {
        return evidence;
    }

    public Person responsible() {
        return responsible;
    }

    public List<CorrectiveAction> actions() {
        return actions.asList();
    }

    List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new FindingAlreadyClosedException();
        }
    }
}
