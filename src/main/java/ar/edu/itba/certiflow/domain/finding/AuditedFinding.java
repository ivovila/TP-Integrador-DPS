package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditAction;
import ar.edu.itba.certiflow.domain.audit.AuditTrail;
import ar.edu.itba.certiflow.domain.evaluation.Evidence;
import ar.edu.itba.certiflow.domain.evaluation.Severity;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.schema.Criterion;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

final class AuditedFinding implements Finding {

    private static final Map<VerificationResult, FindingAudit> AUDIT_BY_RESULT = Map.of(
            VerificationResult.ACCEPTED, FindingAudit.VERIFICATION_ACCEPTED,
            VerificationResult.REJECTED, FindingAudit.VERIFICATION_REJECTED);

    private final Finding finding;
    private final AuditTrail audit;

    AuditedFinding(Finding finding, AuditTrail audit) {
        this.finding = finding;
        this.audit = audit;
    }

    @Override
    public CorrectiveAction planAction(String description, Person responsible, LocalDate dueDate, Person by,
                                       Instant at) {
        CorrectiveAction action = finding.planAction(description, responsible, dueDate, by, at);
        record(FindingAudit.ACTION_PLANNED, by, at, description + ", due " + dueDate);
        return action;
    }

    @Override
    public Verification verifyAction(CorrectiveAction action, Person verifier, VerificationResult result, String notes,
                                     Instant at) {
        Verification verification = finding.verifyAction(action, verifier, result, notes, at);
        record(AUDIT_BY_RESULT.get(result), verifier, at, notes);
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
    public Inspection inspection() {
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
    public List<Evidence> evidence() {
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

    private void record(AuditAction action, Person by, Instant at, String detail) {
        audit.record(this, action, by, at, detail);
    }
}
