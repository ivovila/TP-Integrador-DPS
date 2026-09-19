package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedFindingGenerator {

    private final AuditLog<Finding> auditLog;

    public AuditedFindingGenerator(AuditLog<Finding> auditLog) {
        this.auditLog = auditLog;
    }

    public Finding generate(CriterionResponse<?> nonConformity, InspectionView inspection, Person raisedBy, Instant raisedAt) {
        Asset asset = inspection.asset();
        Finding finding = new AuditedFinding(new StandardFinding(inspection, nonConformity, asset.responsible()), auditLog);
        auditLog.record(finding, new AuditEntry(FindingAudit.RAISED, raisedBy, raisedAt,
                nonConformity.criterion().code() + " " + nonConformity.outcome()));
        return finding;
    }
}
