package ar.edu.itba.certiflow.inspection.model;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.catalog.model.Subsystem;
import ar.edu.itba.certiflow.catalog.model.SubsystemCode;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetLacksRequiredSubsystemsException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotAssignInspectionException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssetResponsibleCannotInspectOwnAssetException;
import ar.edu.itba.certiflow.inspection.model.exceptions.AssignerCannotInspectException;
import ar.edu.itba.certiflow.inspection.model.exceptions.CriterionOutOfScopeException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionAlreadyClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotClosedException;
import ar.edu.itba.certiflow.inspection.model.exceptions.RectificationReasonRequiredException;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.schema.model.Criterion;
import ar.edu.itba.certiflow.schema.model.Evidence;
import ar.edu.itba.certiflow.schema.model.SchemaVersion;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditTrail;
import ar.edu.itba.certiflow.shared.model.Person;
import ar.edu.itba.certiflow.shared.model.Stamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Inspection implements InspectionView {

    private static final String CLOSURE_REASON = "Inspection closed";

    private final InspectionId id;
    private final AssetCode assetCode;
    private final AssetType assetType;
    private final Person assetResponsible;
    private final InspectionAssignment assignment;
    private final SchemaVersion schemaVersion;
    private final Map<Criterion<?>, CriterionResponse<?>> responses = new LinkedHashMap<>();
    private Revisions revisions = new Revisions();
    private final AuditTrail auditTrail = new AuditTrail();

    Inspection(InspectionId id, Asset asset, InspectionAssignment assignment, SchemaVersion schemaVersion, Stamp assigned) {
        if (!asset.subsystems().stream().map(Subsystem::code).collect(Collectors.toSet())
                .containsAll(schemaVersion.requiredSubsystems())) {
            throw new AssetLacksRequiredSubsystemsException(asset.code());
        }
        Person assignedBy = assigned.by();
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
        this.assetType = asset.type();
        this.assetResponsible = asset.responsible();
        this.assignment = assignment;
        this.schemaVersion = schemaVersion;
        assignment.scope().criteriaIn(schemaVersion).forEach(criterion -> responses.put(criterion, new CriterionResponse<>(criterion)));
        auditTrail.record(InspectionAudit.ASSIGNED, assigned, "Schema version " + schemaVersion.number());
    }

    Inspection(InspectionSnapshot snapshot) {
        this.id = snapshot.id();
        this.assetCode = snapshot.assetCode();
        this.assetType = snapshot.assetType();
        this.assetResponsible = snapshot.assetResponsible();
        this.assignment = snapshot.assignment();
        this.schemaVersion = snapshot.schemaVersion();
        snapshot.responses().forEach(response -> responses.put(response.criterion(), response));
        this.revisions = snapshot.revisions();
    }

    public InspectionSnapshot snapshot() {
        return new InspectionSnapshot(id, assetCode, assetType, assetResponsible, assignment, schemaVersion, List.copyOf(responses.values()),
                revisions);
    }

    <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Stamp answered) {
        ensureOpen();
        replaceAnswer(criterion, answer, answered);
        auditTrail.record(InspectionAudit.ANSWER_RECORDED, answered, criterion.code() + " = " + answer);
    }

    void attachEvidence(Criterion<?> criterion, Evidence evidence, Stamp attached) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withEvidence(evidence, attached));
        auditTrail.record(InspectionAudit.EVIDENCE_ATTACHED, attached, criterion.code() + ": " + evidence.reference());
    }

    void addObservation(Criterion<?> criterion, String observationText, Stamp observed) {
        ensureOpen();
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).withObservation(new Observation(observationText, observed)));
        auditTrail.record(InspectionAudit.OBSERVATION_ADDED, observed, criterion.code() + ": " + observationText);
    }

    InspectionClosed close(Stamp closed) {
        ensureOpen();
        Evaluation evaluation = evaluate();
        evaluation.ensureComplete();
        revisions = revisions.sealedWith(CLOSURE_REASON, evaluation, closed);
        auditTrail.record(InspectionAudit.CLOSED, closed, "");
        return new InspectionClosed(id, assetCode, assetResponsible, evaluation.nonConformities(), closed, schemaVersion);
    }

    <A extends Answer> InspectionRectified rectify(Criterion<A> criterion, A answer, String reason, Stamp rectified) {
        ensureClosed();
        if (reason.isBlank()) {
            throw new RectificationReasonRequiredException();
        }
        replaceAnswer(criterion, answer, rectified);
        Evaluation evaluation = evaluate();
        revisions = revisions.sealedWith(reason, evaluation, rectified);
        auditTrail.record(InspectionAudit.RECTIFIED, rectified, criterion.code() + " = " + answer + " (" + reason + ")");
        return new InspectionRectified(id, assetCode, assetResponsible, evaluation.nonConformities().stream()
                .filter(nonConformity -> nonConformity.isFor(criterion)).toList(), rectified, schemaVersion);
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

    /** Cubre un subsistema cuando su alcance incluye todos los criterios que la versión le asigna a ese subsistema. */
    public boolean covers(SubsystemCode subsystem) {
        return responses.keySet().containsAll(schemaVersion.criteriaFor(subsystem));
    }

    @Override
    public boolean isClosed() {
        return !revisions.isEmpty();
    }

    @Override
    public Optional<Instant> closedAt() {
        return revisions.firstSealedAt();
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
    public AssetType assetType() {
        return assetType;
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

    private <A extends Answer> void replaceAnswer(Criterion<A> criterion, A answer, Stamp answered) {
        ensureInScope(criterion);
        responses.put(criterion, responses.get(criterion).answeredWith(criterion, answer, answered));
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
