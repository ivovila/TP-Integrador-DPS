package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionClosed;
import java.util.List;
import java.util.function.Supplier;

public final class AuditedFindingFactory {

    private final AuditLog<Finding> auditLog;

    public AuditedFindingFactory(AuditLog<Finding> auditLog) {
        this.auditLog = auditLog;
    }

    public List<Finding> create(InspectionClosed closure, Supplier<FindingId> nextId) {
        return closure.nonConformities().stream()
                .map(nonConformity -> create(nextId.get(), nonConformity, closure))
                .toList();
    }

    private Finding create(FindingId id, CriterionResponse<?> nonConformity, InspectionClosed closure) {
        Finding finding = new AuditedFinding(new StandardFinding(id, closure.inspectionId(), closure.asset(), nonConformity), auditLog);
        auditLog.record(finding, new AuditEntry(FindingAudit.RAISED, closure.closedBy(), closure.closedAt(),
                nonConformity.criterion().code() + " " + nonConformity.outcome()));
        return finding;
    }
}
