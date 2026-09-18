package ar.edu.itba.certiflow.domain.inspection;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.evaluation.Answer;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.schema.SchemaVersion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.List;

/**
 * Decorator: registra cada operación después de que la inspección decorada la aceptó.
 * Si la operación lanza una excepción, no queda entrada de auditoría.
 */
final class AuditedInspection implements Inspection {

    private final Inspection inspection;
    private final AuditService auditService;

    AuditedInspection(Inspection inspection, AuditService auditService) {
        this.inspection = inspection;
        this.auditService = auditService;
    }

    @Override
    public <A extends Answer> void recordAnswer(Criterion<A> criterion, A answer, Person by, Instant at) {
        inspection.recordAnswer(criterion, answer, by, at);
        record(InspectionAudit.ANSWER_RECORDED, by, at, criterion.code() + " = " + answer);
    }

    @Override
    public void attachEvidence(Criterion<?> criterion, Evidence evidence, Person by, Instant at) {
        inspection.attachEvidence(criterion, evidence, by, at);
        record(InspectionAudit.EVIDENCE_ATTACHED, by, at, criterion.code() + ": " + evidence.reference());
    }

    @Override
    public void addObservation(Criterion<?> criterion, String text, Person by, Instant at) {
        inspection.addObservation(criterion, text, by, at);
        record(InspectionAudit.OBSERVATION_ADDED, by, at, criterion.code() + ": " + text);
    }

    @Override
    public void close(Person by, Instant at) {
        inspection.close(by, at);
        record(InspectionAudit.CLOSED, by, at, "");
    }

    @Override
    public <A extends Answer> void rectify(Criterion<A> criterion, A answer, String reason, Person by, Instant at) {
        inspection.rectify(criterion, answer, reason, by, at);
        record(InspectionAudit.RECTIFIED, by, at, criterion.code() + " = " + answer + " (" + reason + ")");
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

    private void record(InspectionAudit action, Person by, Instant at, String detail) {
        auditService.record(new AuditEntry(this, action, by, at, detail));
    }
}
