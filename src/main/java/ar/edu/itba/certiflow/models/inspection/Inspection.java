package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.evaluation.CriterionEvaluationResult;
import ar.edu.itba.certiflow.models.evaluation.EvaluationRequest;
import ar.edu.itba.certiflow.models.evidence.Evidence;
import ar.edu.itba.certiflow.models.finding.Finding;
import ar.edu.itba.certiflow.models.finding.FindingGenerator;
import ar.edu.itba.certiflow.models.observation.Observation;
import ar.edu.itba.certiflow.models.people.Inspector;
import ar.edu.itba.certiflow.models.schema.InspectionSchema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Inspection {
    private static final UUID EVALUATION_RECORDED = UUID.fromString("c6a3bd93-f4df-43c5-b845-2e8e95f21002");
    private static final UUID EVIDENCE_COLLECTED = UUID.fromString("0ef773b8-4289-4b81-b3d1-1c71ca5a1003");
    private static final UUID OBSERVATION_ADDED = UUID.fromString("e1e1a44d-069b-4f5f-8a26-af97e2d11004");
    private static final UUID CLOSED = UUID.fromString("aaafc344-68f7-46f0-a490-a3040dd01005");
    private static final UUID RECTIFIED = UUID.fromString("b83c01cb-83cf-48e5-b827-3b93058b1006");

    private final UUID id;
    private final Asset asset;
    private final InspectionSchema schema;
    private final Inspector inspector;
    private final LocalDate scheduledDate;
    private final String scope;
    private final UUID rectifiedInspectionId;
    private final Rectification rectification;
    private final List<CriterionEvaluationResult<?>> evaluations = new ArrayList<>();
    private final List<Evidence> evidences = new ArrayList<>();
    private final List<Observation> observations = new ArrayList<>();
    private final List<Finding> findings = new ArrayList<>();
    private final List<InspectionAuditEntry> audit = new ArrayList<>();
    private Instant closedAt;

    public Inspection(
            Asset asset,
            InspectionSchema schema,
            Inspector inspector,
            LocalDate scheduledDate,
            String scope) {
        this(UUID.randomUUID(), asset, schema, inspector, scheduledDate, scope, null, null);
    }

    private Inspection(
            UUID id,
            Asset asset,
            InspectionSchema schema,
            Inspector inspector,
            LocalDate scheduledDate,
            String scope,
            UUID rectifiedInspectionId,
            Rectification rectification) {
        this.id = id;
        this.asset = asset;
        this.schema = schema;
        this.inspector = inspector;
        this.scheduledDate = scheduledDate;
        this.scope = scope;
        this.rectifiedInspectionId = rectifiedInspectionId;
        this.rectification = rectification;
    }

    public <T> void evaluate(EvaluationRequest<T> request) {
        ensureOpen();
        if (!schema.includes(request.criterion())) {
            throw new CriterionNotInInspectionSchemaException();
        }
        evaluations.add(request.evaluate());
        audit(EVALUATION_RECORDED, inspector, "");
    }

    public void collectEvidence(Evidence evidence) {
        ensureOpen();
        evidences.add(evidence);
        audit(EVIDENCE_COLLECTED, inspector, "");
    }

    public void addObservation(Observation observation) {
        ensureOpen();
        observations.add(observation);
        audit(OBSERVATION_ADDED, inspector, "");
    }

    public void close(FindingGenerator generator, Inspector actor) {
        ensureOpen();
        findings.addAll(generator.generateFrom(evaluations));
        closedAt = Instant.now();
        audit(CLOSED, actor, "");
    }

    public Inspection rectify(Rectification rectification) {
        if (!closed()) {
            throw new InspectionOperationNotAllowedException();
        }
        var rectifiedInspection = new Inspection(UUID.randomUUID(), asset, schema, rectification.actor(),
                scheduledDate, scope, id, rectification);
        rectifiedInspection.audit(RECTIFIED, rectification.actor(), rectification.reason());
        return rectifiedInspection;
    }

    public UUID id() { return id; }
    public Asset asset() { return asset; }
    public InspectionSchema schema() { return schema; }
    public Inspector inspector() { return inspector; }
    public LocalDate scheduledDate() { return scheduledDate; }
    public String scope() { return scope; }
    public UUID rectifiedInspectionId() { return rectifiedInspectionId; }
    public Rectification rectification() { return rectification; }
    public boolean closed() { return closedAt != null; }
    public Instant closedAt() { return closedAt; }
    public Collection<CriterionEvaluationResult<?>> evaluations() { return evaluations; }
    public Collection<Evidence> evidences() { return evidences; }
    public Collection<Observation> observations() { return observations; }
    public Collection<Finding> findings() { return findings; }
    public Collection<InspectionAuditEntry> audit() { return audit; }

    private void audit(UUID eventId, Inspector actor, String detail) {
        audit.add(new InspectionAuditEntry(UUID.randomUUID(), eventId, actor, Instant.now(), detail));
    }

    private void ensureOpen() {
        if (closed()) {
            throw new InspectionOperationNotAllowedException();
        }
    }
}
