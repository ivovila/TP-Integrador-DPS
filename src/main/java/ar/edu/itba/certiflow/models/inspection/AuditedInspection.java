package ar.edu.itba.certiflow.models.inspection;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditAction;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.evaluation.Answer;
import ar.edu.itba.certiflow.models.evaluation.Evidence;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.schema.SchemaVersion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.List;

final class AuditedInspection implements Inspection {

    private final Inspection inspection;
    private final AuditTrail auditTrail;

    AuditedInspection(Inspection inspection, AuditTrail auditTrail) {
        this.inspection = inspection;
        this.auditTrail = auditTrail;
    }

    @Override
    public <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person answeredBy, Instant answeredAt) {
        inspection.recordAnswer(criterion, answer, answeredBy, answeredAt);
        record(InspectionAudit.ANSWER_RECORDED, answeredBy, answeredAt, criterion.code() + " = " + answer);
    }

    @Override
    public void attachEvidence(Criterion<?> criterion, Evidence evidence, Person attachedBy, Instant attachedAt) {
        inspection.attachEvidence(criterion, evidence, attachedBy, attachedAt);
        record(InspectionAudit.EVIDENCE_ATTACHED, attachedBy, attachedAt, criterion.code() + ": " + evidence.reference());
    }

    @Override
    public void addObservation(Criterion<?> criterion, String observationText, Person observedBy, Instant observedAt) {
        inspection.addObservation(criterion, observationText, observedBy, observedAt);
        record(InspectionAudit.OBSERVATION_ADDED, observedBy, observedAt, criterion.code() + ": " + observationText);
    }

    @Override
    public void close(Person closedBy, Instant closedAt) {
        inspection.close(closedBy, closedAt);
        record(InspectionAudit.CLOSED, closedBy, closedAt, "");
    }

    @Override
    public <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person rectifiedBy, Instant rectifiedAt) {
        inspection.rectify(criterion, answer, reason, rectifiedBy, rectifiedAt);
        record(InspectionAudit.RECTIFIED, rectifiedBy, rectifiedAt, criterion.code() + " = " + answer + " (" + reason + ")");
    }

    @Override
    public Evaluation evaluate() {
        return inspection.evaluate();
    }

    @Override
    public boolean isClosed() {
        return inspection.isClosed();
    }

    @Override
    public Revision originalRevision() {
        return inspection.originalRevision();
    }

    @Override
    public List<Revision> revisions() {
        return inspection.revisions();
    }

    @Override
    public Asset asset() {
        return inspection.asset();
    }

    @Override
    public InspectionAssignment assignment() {
        return inspection.assignment();
    }

    @Override
    public SchemaVersion schemaVersion() {
        return inspection.schemaVersion();
    }

    private void record(AuditAction auditAction, Person performedBy, Instant occurredAt, String detail) {
        auditTrail.record(this, auditAction, performedBy, occurredAt, detail);
    }
}
