package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionClosed;
import java.util.List;

public final class AuditedFindingGenerator {

    private final AuditLog<Finding> auditLog;

    public AuditedFindingGenerator(AuditLog<Finding> auditLog) {
        this.auditLog = auditLog;
    }

    public List<Finding> generate(InspectionClosed closure) {
        return closure.nonConformities().stream()
                .map(nonConformity -> generate(nonConformity, closure))
                .toList();
    }

    private Finding generate(CriterionResponse<?> nonConformity, InspectionClosed closure) {
        Finding finding = new AuditedFinding(new StandardFinding(closure.inspection(), nonConformity,
                closure.inspection().asset().responsible()), auditLog);
        auditLog.record(finding, new AuditEntry(FindingAudit.RAISED, closure.closedBy(), closure.closedAt(),
                nonConformity.criterion().code() + " " + nonConformity.outcome()));
        return finding;
    }
}
