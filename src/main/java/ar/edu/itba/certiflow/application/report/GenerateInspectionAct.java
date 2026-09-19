package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.domain.asset.Asset;
import ar.edu.itba.certiflow.domain.audit.AuditService;
import ar.edu.itba.certiflow.domain.inspection.CriterionResponse;
import ar.edu.itba.certiflow.domain.inspection.GivenAnswer;
import ar.edu.itba.certiflow.domain.inspection.Inspection;
import ar.edu.itba.certiflow.domain.inspection.InspectionNotClosedException;
import ar.edu.itba.certiflow.domain.inspection.Observation;
import java.util.List;

public final class GenerateInspectionAct {

    private final AuditService auditService;

    public GenerateInspectionAct(AuditService auditService) {
        this.auditService = auditService;
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
        GivenAnswer<?> given = response.given().orElseThrow();
        return new ActLine(response.criterion().code(), response.criterion().text(), given.value(), given.by(),
                given.at(), response.attachments().size(),
                response.observations().stream().map(Observation::text).toList(), response.outcome());
    }
}
