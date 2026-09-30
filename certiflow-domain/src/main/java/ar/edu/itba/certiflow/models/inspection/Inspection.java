package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssetResponsibleCannotAssignInspectionException;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssetResponsibleCannotInspectOwnAssetException;
import ar.edu.itba.certiflow.models.inspection.exceptions.AssignerCannotInspectException;
import ar.edu.itba.certiflow.models.inspection.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.models.inspection.exceptions.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Inspection implements InspectionView {

    private static final String CLOSURE_REASON = "Inspection closed";

    private final InspectionId id;
    private final Asset asset;
    private final InspectionAssignment assignment;
    private final SchemaVersion schemaVersion;
    private final Map<Criterion<?>, CriterionResponse<?>> responses = new LinkedHashMap<>();
    private final List<Revision> revisions = new ArrayList<>();
    private final AuditTrail auditTrail = new AuditTrail();

    Inspection(InspectionId id, Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion, Person assignedBy,
               Instant assignedAt) {
        Person inspector = assignment.inspector();
        if (assignedBy.equals(inspector)) {
            throw new AssignerCannotInspectException(inspector);
        }
        if (assignedBy.equals(asset.responsible())) {
            throw new AssetResponsibleCannotAssignInspectionException(assignedBy);
        }
        if (inspector.equals(asset.responsible())) {
            throw new AssetResponsibleCannotInspectOwnAssetException(inspector);
        }
        this.id = id;
        this.asset = asset;
        this.assignment = assignment;
        this.schemaVersion = schemaVersion;
        assignment.scope().criteriaIn(schemaVersion).forEach(criterion -> responses.put(criterion, new CriterionResponse<>(criterion)));
        auditTrail.record(InspectionAudit.ASSIGNED, assignedBy, assignedAt, "Schema version " + schemaVersion.number());
    }

    public <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person answeredBy, Instant answeredAt) {
        ensureOpen();
        replaceAnswer(criterion, answer, answeredBy, answeredAt);
        auditTrail.record(InspectionAudit.ANSWER_RECORDED, answeredBy, answeredAt, criterion.code() + " = " + answer);
    }

    public void attachEvidence(Criterion<?> criterion, Evidence evidence, Person attachedBy, Instant attachedAt) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withEvidence(evidence, attachedBy, attachedAt));
        auditTrail.record(InspectionAudit.EVIDENCE_ATTACHED, attachedBy, attachedAt, criterion.code() + ": " + evidence.reference());
    }

    public void addObservation(Criterion<?> criterion, String observationText, Person observedBy, Instant observedAt) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withObservation(new Observation(observationText, observedBy, observedAt)));
        auditTrail.record(InspectionAudit.OBSERVATION_ADDED, observedBy, observedAt, criterion.code() + ": " + observationText);
    }

    public InspectionClosed close(Person closedBy, Instant closedAt) {
        ensureOpen();
        Evaluation evaluation = evaluate();
        evaluation.ensureComplete();
        revisions.add(new Revision(1, CLOSURE_REASON, evaluation, closedBy, closedAt));
        auditTrail.record(InspectionAudit.CLOSED, closedBy, closedAt, "");
        return new InspectionClosed(id, asset, evaluation.nonConformities(), closedBy, closedAt);
    }

    public <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person rectifiedBy, Instant rectifiedAt) {
        ensureClosed();
        if (reason.isBlank()) {
            throw new RectificationReasonRequiredException();
        }
        replaceAnswer(criterion, answer, rectifiedBy, rectifiedAt);
        revisions.add(new Revision(revisions.size() + 1, reason, evaluate(), rectifiedBy, rectifiedAt));
        auditTrail.record(InspectionAudit.RECTIFIED, rectifiedBy, rectifiedAt, criterion.code() + " = " + answer + " (" + reason + ")");
    }

    public List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    @Override
    public Evaluation evaluate() {
        return new Evaluation(List.copyOf(responses.values()));
    }

    @Override
    public boolean coversWholeSchema() {
        return responses.size() == schemaVersion.criteria().size();
    }

    @Override
    public boolean isClosed() {
        return !revisions.isEmpty();
    }

    @Override
    public Revision originalRevision() {
        ensureClosed();
        return revisions.getFirst();
    }

    @Override
    public List<Revision> revisions() {
        return List.copyOf(revisions);
    }

    @Override
    public InspectionId id() {
        return id;
    }

    @Override
    public Asset asset() {
        return asset;
    }

    @Override
    public InspectionAssignment assignment() {
        return assignment;
    }

    @Override
    public SchemaVersion schemaVersion() {
        return schemaVersion;
    }

    private <A extends Answer> void replaceAnswer(Criterion<A> criterion, A answer, Person answeredBy, Instant answeredAt) {
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).answeredWith(criterion, answer, answeredBy, answeredAt));
    }

    private void ensureInScope(Criterion<?> criterion) {
        schemaVersion.ensureContains(criterion);
        if (!responses.containsKey(criterion)) {
            throw new CriterionOutOfScopeException(criterion);
        }
    }

    private void ensureOpen() {
        if (isClosed()) {
            throw new InspectionAlreadyClosedException();
        }
    }

    private void ensureClosed() {
        if (!isClosed()) {
            throw new InspectionNotClosedException();
        }
    }
}
