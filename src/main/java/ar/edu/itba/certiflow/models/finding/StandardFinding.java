package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
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

final class StandardFinding implements Finding {

    private final FindingId id;
    private final InspectionId inspectionId;
    private final Asset asset;
    private final Criterion<?> criterion;
    private final List<AttachedEvidence> evidence;
    private final Person responsible;
    private final List<CorrectiveAction> actions = new ArrayList<>();

    StandardFinding(FindingId id, InspectionId inspectionId, Asset asset, CriterionResponse<?> nonConformity) {
        this.id = id;
        this.inspectionId = inspectionId;
        this.asset = asset;
        this.responsible = asset.responsible();
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.attachments();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
    }

    @Override
    public CorrectiveAction planAction(String description, Person actionResponsible, LocalDate dueDate, Person plannedBy,
                                       Instant plannedAt, LocalDate plannedOn) {
        ensureOpen();
        CorrectiveAction plannedAction = new CorrectiveAction(new CorrectiveActionId(actions.size() + 1), description,
                actionResponsible, dueDate, plannedBy, plannedAt, plannedOn);
        actions.add(plannedAction);
        return plannedAction;
    }

    @Override
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
        return verification;
    }

    @Override
    public boolean isOpen() {
        return actions.stream().noneMatch(CorrectiveAction::isClosed);
    }

    @Override
    public boolean blocksCertification() {
        return isOpen() && severity().blocksCertification();
    }

    @Override
    public boolean concerns(Asset candidate) {
        return asset.equals(candidate);
    }

    @Override
    public boolean raisedFrom(InspectionId sourceInspection, CriterionResponse<?> nonConformity) {
        return inspectionId.equals(sourceInspection) && nonConformity.isFor(criterion);
    }

    @Override
    public FindingId id() {
        return id;
    }

    @Override
    public InspectionId inspectionId() {
        return inspectionId;
    }

    @Override
    public Criterion<?> criterion() {
        return criterion;
    }

    @Override
    public Severity severity() {
        return criterion.severity();
    }

    @Override
    public List<AttachedEvidence> evidence() {
        return evidence;
    }

    @Override
    public Person responsible() {
        return responsible;
    }

    @Override
    public List<CorrectiveAction> actions() {
        return List.copyOf(actions);
    }

    private void ensureOpen() {
        if (!isOpen()) {
            throw new FindingAlreadyClosedException();
        }
    }
}
