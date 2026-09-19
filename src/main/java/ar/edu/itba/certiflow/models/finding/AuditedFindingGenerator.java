package ar.edu.itba.certiflow.models.finding;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditService;
import ar.edu.itba.certiflow.models.audit.AuditTrail;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.InspectionView;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;

public final class AuditedFindingGenerator {

    private final AuditTrail auditTrail;

    public AuditedFindingGenerator(AuditService auditService) {
        this.auditTrail = new AuditTrail(auditService);
    }

    public Finding generate(CriterionResponse<?> nonConformity, InspectionView inspection, Person raisedBy, Instant raisedAt) {
        Asset asset = inspection.asset();
        Finding finding = new AuditedFinding(new StandardFinding(inspection, nonConformity, asset.responsible()), auditTrail);
        auditTrail.record(finding, FindingAudit.RAISED, raisedBy, raisedAt,
                nonConformity.criterion().code() + " " + nonConformity.outcome());
        return finding;
    }
}
