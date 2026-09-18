package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.inspection.CriterionResponse;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionNotClosedException;
import ar.edu.itba.certiflow.domain.inspection.Observation;
import java.util.List;
import java.util.Objects;

public final class GenerateInspectionAct {

    private final AuditService auditService;

    public GenerateInspectionAct(AuditService auditService) {
        this.auditService = Objects.requireNonNull(auditService, "auditService");
    }

    public InspectionAct execute(Inspection inspection) {
        if (!inspection.isClosed()) {
            throw new InspectionNotClosedException();
        }
        Asset asset = inspection.asset();
        List<ActLine> lines = inspection.evaluate().responses().stream().map(this::lineOf).toList();
        return new InspectionAct(asset.code(), asset.name(), inspection.assignment(),
                inspection.schemaVersion().number(), lines, inspection.revisions(),
                auditService.entriesFor(inspection));
    }

    private ActLine lineOf(CriterionResponse<?> response) {
        return new ActLine(response.criterion().code(), response.criterion().text(), response.answer().orElseThrow(),
                response.evidence().size(), response.observations().stream().map(Observation::text).toList(),
                response.outcome());
    }
}
