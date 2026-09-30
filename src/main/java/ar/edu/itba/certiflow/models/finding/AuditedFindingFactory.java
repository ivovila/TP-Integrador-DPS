package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionClosed;
import java.util.List;

public final class AuditedFindingFactory {

    private final AuditLog<Finding> auditLog;

    public AuditedFindingFactory(AuditLog<Finding> auditLog) {
        this.auditLog = auditLog;
    }

    public List<Finding> create(InspectionClosed closure) {
        return closure.nonConformities().stream()
                .map(nonConformity -> create(nonConformity, closure))
                .toList();
    }

    private Finding create(CriterionResponse<?> nonConformity, InspectionClosed closure) {
        Finding finding = new AuditedFinding(new StandardFinding(closure.inspection(), nonConformity,
                closure.inspection().asset().responsible()), auditLog);
        auditLog.record(finding, new AuditEntry(FindingAudit.RAISED, closure.closedBy(), closure.closedAt(),
                nonConformity.criterion().code() + " " + nonConformity.outcome()));
        return finding;
    }
}
