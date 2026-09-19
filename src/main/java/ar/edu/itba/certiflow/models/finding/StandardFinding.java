package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.finding.exceptions.CorrectiveActionNotInFindingException;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingAlreadyClosedException;
import ar.edu.itba.certiflow.models.finding.exceptions.FindingRequiresNonConformityException;
import ar.edu.itba.certiflow.models.inspection.AttachedEvidence;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class StandardFinding implements Finding {

    private final InspectionView inspection;
    private final Criterion<?> criterion;
    private final List<AttachedEvidence> evidence;
    private final Person responsible;
    private final List<CorrectiveAction> actions = new ArrayList<>();

    StandardFinding(InspectionView inspection, CriterionResponse<?> nonConformity, Person responsible) {
        this.inspection = inspection;
        this.responsible = responsible;
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.attachments();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
    }

    @Override
    public CorrectiveAction planAction(String description, Person actionResponsible, LocalDate dueDate, Person plannedBy,
                                       Instant plannedAt) {
        ensureOpen();
        CorrectiveAction plannedAction = new CorrectiveAction(description, actionResponsible, dueDate, plannedBy, plannedAt);
        actions.add(plannedAction);
        return plannedAction;
    }

    @Override
    public Verification verifyAction(CorrectiveAction correctiveAction, Person verifier, VerificationResult verificationResult, String verificationNotes,
                                     Instant verifiedAt) {
        ensureOpen();
        if (!actions.contains(correctiveAction)) {
            throw new CorrectiveActionNotInFindingException();
        }
        return correctiveAction.verify(verifier, verificationResult, verificationNotes, verifiedAt);
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
    public boolean concerns(Asset asset) {
        return inspection.asset().equals(asset);
    }

    @Override
    public InspectionView inspection() {
        return inspection;
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
