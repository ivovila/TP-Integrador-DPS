package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.finding.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingRequiresNonConformityException;
import ar.edu.itba.certiflow.models.inspection.AttachedEvidence;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class Finding {

    private final FindingId id;
    private final InspectionId inspectionId;
    private final Asset asset;
    private final Criterion<?> criterion;
    private final List<AttachedEvidence> evidence;
    private final Person responsible;
    private final List<CorrectiveAction> actions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();

    Finding(FindingId id, InspectionId inspectionId, Asset asset, CriterionResponse<?> nonConformity, Person raisedBy, Instant raisedAt) {
        this.id = id;
        this.inspectionId = inspectionId;
        this.asset = asset;
        this.responsible = asset.responsible();
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.attachments();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
        auditTrail.record(FindingAudit.RAISED, raisedBy, raisedAt, criterion.code() + " " + nonConformity.outcome());
    }

    public CorrectiveAction planAction(String description, Person actionResponsible, LocalDate dueDate, Person plannedBy,
                                       Instant plannedAt, LocalDate plannedOn) {
        ensureOpen();
        CorrectiveAction plannedAction = new CorrectiveAction(new CorrectiveActionId(actions.size() + 1), description,
                actionResponsible, dueDate, plannedBy, plannedAt, plannedOn);
        actions.add(plannedAction);
        auditTrail.record(FindingAudit.ACTION_PLANNED, plannedBy, plannedAt, description + ", due " + dueDate);
        return plannedAction;
    }

    public Verification verifyAction(CorrectiveActionId actionId, Person verifier, VerificationResult verificationResult,
                                     String verificationNotes, Instant verifiedAt) {
        ensureOpen();
        CorrectiveAction correctiveAction = actions.stream()
                .filter(action -> action.isIdentifiedBy(actionId))
                .findFirst()
                .orElseThrow(() -> new CorrectiveActionNotInFindingException(actionId));
        Verification verification = correctiveAction.verify(verifier, verificationResult, verificationNotes, verifiedAt);
        if (verification.isAccepted()) {
            actions.stream().filter(CorrectiveAction::isOpen).forEach(openAction -> openAction.cancel(verifier, verifiedAt));
        }
        auditTrail.record(verificationResult.audit(), verifier, verifiedAt, verificationNotes);
        return verification;
    }

    public boolean isOpen() {
        return actions.stream().noneMatch(CorrectiveAction::isClosed);
    }

    public boolean blocksCertification() {
        return isOpen() && severity().blocksCertification();
    }

    public boolean concerns(Asset candidate) {
        return asset.equals(candidate);
    }

    public boolean raisedFrom(InspectionId sourceInspection, CriterionResponse<?> nonConformity) {
        return inspectionId.equals(sourceInspection) && nonConformity.isFor(criterion);
    }

    public FindingId id() {
        return id;
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
        return List.copyOf(actions);
    }

    public List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new FindingAlreadyClosedException();
        }
    }
}
