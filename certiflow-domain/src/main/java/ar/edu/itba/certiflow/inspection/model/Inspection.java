package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotAssignInspectionException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotInspectOwnAssetException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssignerCannotInspectException;
import ar.edu.itba.certiflow.inspection.model.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.Person;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Inspection implements InspectionView {

    private static final String CLOSURE_REASON = "Inspection closed";

    private final InspectionId id;
    private final AssetCode assetCode;
    private final Person assetResponsible;
    private final InspectionAssignment assignment;
    private final SchemaVersion schemaVersion;
    private final Map<Criterion<?>, CriterionResponse<?>> responses = new LinkedHashMap<>();
    private Revisions revisions = Revisions.none();
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
        this.assetCode = asset.code();
        this.assetResponsible = asset.responsible();
        this.assignment = assignment;
        this.schemaVersion = schemaVersion;
        assignment.scope().criteriaIn(schemaVersion).forEach(criterion -> responses.put(criterion, new CriterionResponse<>(criterion)));
        auditTrail.record(InspectionAudit.ASSIGNED, assignedBy, assignedAt, "Schema version " + schemaVersion.number());
    }

    <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person answeredBy, Instant answeredAt) {
        ensureOpen();
        replaceAnswer(criterion, answer, answeredBy, answeredAt);
        auditTrail.record(InspectionAudit.ANSWER_RECORDED, answeredBy, answeredAt, criterion.code() + " = " + answer);
    }

    void attachEvidence(Criterion<?> criterion, Evidence evidence, Person attachedBy, Instant attachedAt) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withEvidence(evidence, attachedBy, attachedAt));
        auditTrail.record(InspectionAudit.EVIDENCE_ATTACHED, attachedBy, attachedAt, criterion.code() + ": " + evidence.reference());
    }

    void addObservation(Criterion<?> criterion, String observationText, Person observedBy, Instant observedAt) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withObservation(new Observation(observationText, observedBy, observedAt)));
        auditTrail.record(InspectionAudit.OBSERVATION_ADDED, observedBy, observedAt, criterion.code() + ": " + observationText);
    }

    InspectionClosed close(Person closedBy, Instant closedAt) {
        ensureOpen();
        Evaluation evaluation = evaluate();
        evaluation.ensureComplete();
        revisions = revisions.sealedWith(CLOSURE_REASON, evaluation, closedBy, closedAt);
        auditTrail.record(InspectionAudit.CLOSED, closedBy, closedAt, "");
        return new InspectionClosed(id, assetCode, assetResponsible, evaluation.nonConformities(), closedBy, closedAt);
    }

    <A extends Answer> InspectionRectified rectify(Criterion<A> criterion, A answer, String reason, Person rectifiedBy, Instant rectifiedAt) {
        ensureClosed();
        if (reason.isBlank()) {
            throw new RectificationReasonRequiredException();
        }
        replaceAnswer(criterion, answer, rectifiedBy, rectifiedAt);
        Evaluation evaluation = evaluate();
        revisions = revisions.sealedWith(reason, evaluation, rectifiedBy, rectifiedAt);
        auditTrail.record(InspectionAudit.RECTIFIED, rectifiedBy, rectifiedAt, criterion.code() + " = " + answer + " (" + reason + ")");
        return new InspectionRectified(id, assetCode, assetResponsible, evaluation.nonConformities().stream()
                .filter(nonConformity -> nonConformity.isFor(criterion)).toList(), rectifiedBy, rectifiedAt);
    }

    List<AuditEntry> pullAuditEntries() {
        return auditTrail.pull();
    }

    /** Un acta solo se emite sobre una inspección cerrada. */
    public void ensureClosed() {
        if (!isClosed()) {
            throw new InspectionNotClosedException();
        }
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
        return revisions.original();
    }

    @Override
    public Revisions revisions() {
        return revisions;
    }

    @Override
    public InspectionId id() {
        return id;
    }

    @Override
    public AssetCode assetCode() {
        return assetCode;
    }

    @Override
    public Person assetResponsible() {
        return assetResponsible;
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

}
