package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.inspection.CriterionResponse;
import ar.edu.itba.certiflow.domain.inspection.InspectionView;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class StandardFinding implements Finding {

    private final InspectionView inspection;
    private final Criterion<?> criterion;
    private final List<Evidence> evidence;
    private final Person responsible;
    private final List<CorrectiveAction> actions = new ArrayList<>();

    StandardFinding(InspectionView inspection, CriterionResponse<?> nonConformity, Person responsible) {
        this.inspection = inspection;
        this.responsible = responsible;
        this.criterion = nonConformity.criterion();
        this.evidence = nonConformity.evidence();
        if (!nonConformity.outcome().raisesFinding()) {
            throw new FindingRequiresNonConformityException(criterion);
        }
    }

    @Override
    public CorrectiveAction planAction(String description, Person actionResponsible, LocalDate dueDate, Person by,
                                       Instant at) {
        ensureOpen();
        CorrectiveAction action = new CorrectiveAction(description, actionResponsible, dueDate, by, at);
        actions.add(action);
        return action;
    }

    @Override
    public Verification verifyAction(CorrectiveAction action, Person verifier, VerificationResult result, String notes,
                                     Instant at) {
        ensureOpen();
        if (!actions.contains(action)) {
            throw new CorrectiveActionNotInFindingException();
        }
        return action.verify(verifier, result, notes, at);
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
    public List<Evidence> evidence() {
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
