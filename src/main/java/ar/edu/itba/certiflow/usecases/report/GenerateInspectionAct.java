package ar.edu.itba.certiflow.usecases.report;

import ar.edu.itba.certiflow.application.report.ActLine;
import ar.edu.itba.certiflow.application.report.InspectionAct;
import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.GivenAnswer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.Observation;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotClosedException;
import java.util.List;

public final class GenerateInspectionAct {

    private final AuditLog<Inspection> inspectionLog;

    public GenerateInspectionAct(AuditLog<Inspection> inspectionLog) {
        this.inspectionLog = inspectionLog;
    }

    public InspectionAct execute(Inspection inspection) {
        if (!inspection.isClosed()) {
            throw new InspectionNotClosedException();
        }
        Asset asset = inspection.asset();
        List<ActLine> actLines = inspection.evaluate().responses().stream().map(this::lineOf).toList();
        return new InspectionAct(asset.code(), asset.name(), inspection.assignment(),
                inspection.schemaVersion().number(), actLines, inspection.revisions(),
                inspectionLog.historyOf(inspection));
    }

    private ActLine lineOf(CriterionResponse<?> response) {
        GivenAnswer<?> givenAnswer = response.given().orElseThrow();
        return new ActLine(response.criterion().code(), response.criterion().text(), givenAnswer.value(), givenAnswer.answeredBy(),
                givenAnswer.answeredAt(), response.attachments().size(),
                response.observations().stream().map(Observation::text).toList(), response.outcome());
    }
}
