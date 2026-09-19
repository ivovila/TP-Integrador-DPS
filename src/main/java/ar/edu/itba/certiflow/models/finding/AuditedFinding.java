package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditAction;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.evaluation.Severity;
import ar.edu.itba.certiflow.models.inspection.AttachedEvidence;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.schema.Criterion;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class AuditedFinding implements Finding {

    private static final Map<VerificationResult, FindingAudit> AUDIT_BY_RESULT = Map.of(
            VerificationResult.ACCEPTED, FindingAudit.VERIFICATION_ACCEPTED,
            VerificationResult.REJECTED, FindingAudit.VERIFICATION_REJECTED);

    private final Finding finding;
    private final AuditLog<Finding> auditLog;

    AuditedFinding(Finding finding, AuditLog<Finding> auditLog) {
        this.finding = finding;
        this.auditLog = auditLog;
    }

    @Override
    public CorrectiveAction planAction(String description, Person responsible, LocalDate dueDate, Person plannedBy,
                                       Instant plannedAt) {
        CorrectiveAction plannedAction = finding.planAction(description, responsible, dueDate, plannedBy, plannedAt);
        record(FindingAudit.ACTION_PLANNED, plannedBy, plannedAt, description + ", due " + dueDate);
        return plannedAction;
    }

    @Override
    public Verification verifyAction(CorrectiveAction correctiveAction, Person verifier, VerificationResult verificationResult, String verificationNotes,
                                     Instant verifiedAt) {
        Verification verification = finding.verifyAction(correctiveAction, verifier, verificationResult, verificationNotes, verifiedAt);
        record(AUDIT_BY_RESULT.get(verificationResult), verifier, verifiedAt, verificationNotes);
        return verification;
    }

    @Override
    public boolean isOpen() {
        return finding.isOpen();
    }

    @Override
    public boolean blocksCertification() {
        return finding.blocksCertification();
    }

    @Override
    public boolean concerns(Asset asset) {
        return finding.concerns(asset);
    }

    @Override
    public InspectionView inspection() {
        return finding.inspection();
    }

    @Override
    public Criterion<?> criterion() {
        return finding.criterion();
    }

    @Override
    public Severity severity() {
        return finding.severity();
    }

    @Override
    public List<AttachedEvidence> evidence() {
        return finding.evidence();
    }

    @Override
    public Person responsible() {
        return finding.responsible();
    }

    @Override
    public List<CorrectiveAction> actions() {
        return finding.actions();
    }

    private void record(AuditAction auditAction, Person performedBy, Instant occurredAt, String detail) {
        auditLog.record(this, new AuditEntry(auditAction, performedBy, occurredAt, detail));
    }
}
