package ar.edu.itba.certiflow.domain.finding;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditEntry;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.inspection.CriterionResponse;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.shared.Person;
import java.time.Instant;
import java.util.Objects;


public final class AuditedFindingGenerator {

    private final AuditService auditService;

    public AuditedFindingGenerator(AuditService auditService) {
        this.auditService = auditService;
    }

    public Finding generate(CriterionResponse<?> nonConformity, Inspection inspection, Person by, Instant at) {
        Asset asset = inspection.asset();
        Finding finding = new AuditedFinding(new StandardFinding(inspection, nonConformity, asset.responsible()),
                auditService);
        auditService.record(new AuditEntry(finding, FindingAudit.RAISED, by, at,
                nonConformity.criterion().code() + " " + nonConformity.outcome()));
        return finding;
    }
}
