package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.SubsystemAssociation;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.FindingRequiresNonConformityException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Severity;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.LocalDate;
import java.util.List;

public final class Finding {

    private final FindingId id;
    private final InspectionId inspectionId;
    private final AssetCode assetCode;
    private final Criterion<?> criterion;
    private final SubsystemAssociation subsystem;
    private final List<AttachedEvidence> evidence;
    private final Person responsible;
    private final CorrectiveActions actions;
    private final AuditTrail auditTrail = new AuditTrail();

    Finding(FindingId id, RaisesFindings source, CriterionResponse<?> nonConformity) {
        this.id = id;
        this.subsystem = source.schemaVersion().associationOf(nonConformity.criterion());
        this.inspectionId = source.inspectionId();
        this.assetCode = source.assetCode();
        this.responsible = source.assetResponsible();
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.attachments();
        this.actions = new CorrectiveActions();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
        auditTrail.record(FindingAudit.RAISED, source.raised(), criterion.code().value() + " " + nonConformity.outcome());
    }

    Finding(FindingSnapshot snapshot) {
        this.id = snapshot.id();
        this.subsystem = snapshot.subsystem();
        this.inspectionId = snapshot.inspectionId();
        this.assetCode = snapshot.assetCode();
        this.criterion = snapshot.criterion();
        this.evidence = snapshot.evidence();
        this.responsible = snapshot.responsible();
        this.actions = new CorrectiveActions(snapshot.actions());
    }

    public FindingSnapshot snapshot() {
        return new FindingSnapshot(id, inspectionId, assetCode, criterion, evidence, responsible, actions.snapshot(), subsystem);
    }

    CorrectiveAction planAction(ActionPlan plan, Stamp planned, LocalDate plannedOn) {
        ensureOpen();
        CorrectiveAction plannedAction = actions.plan(plan, planned, plannedOn);
        auditTrail.record(FindingAudit.ACTION_PLANNED, planned, plan.description() + ", due " + plan.dueDate());
        return plannedAction;
    }

    Verification verifyAction(CorrectiveActionId actionId, VerificationResult verificationResult, String verificationNotes,
                              Stamp verified) {
        ensureOpen();
        CorrectiveAction correctiveAction = actions.withId(actionId);
        Verification verification = correctiveAction.verify(verificationResult, verificationNotes, verified);
        if (verification.isAccepted()) {
            actions.cancelOpen(verified);
        }
        auditTrail.record(verificationResult.audit(), verified, verificationNotes);
        return verification;
    }

    public boolean isOpen() {
        return actions.noneAccepted();
    }


    public boolean concerns(AssetCode candidate) {
        return assetCode.equals(candidate);
    }

    /** Un hallazgo abierto por esa misma no conformidad: evita duplicarlo al rectificar. */
    public boolean isOpenFor(InspectionId sourceInspection, CriterionResponse<?> nonConformity) {
        return isOpen() && inspectionId.equals(sourceInspection) && nonConformity.isFor(criterion);
    }

    public SubsystemAssociation subsystem() {
        return subsystem;
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
