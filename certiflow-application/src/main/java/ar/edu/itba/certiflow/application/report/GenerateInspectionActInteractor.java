package ar.edu.itba.certiflow.application.report;

import ar.edu.itba.certiflow.models.asset.Asset;
import ar.edu.itba.certiflow.models.asset.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.inspection.CriterionResponse;
import ar.edu.itba.certiflow.models.inspection.GivenAnswer;
import ar.edu.itba.certiflow.models.inspection.Inspection;
import ar.edu.itba.certiflow.models.inspection.InspectionId;
import ar.edu.itba.certiflow.models.inspection.Observation;
import ar.edu.itba.certiflow.models.inspection.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.ports.AssetRepository;
import ar.edu.itba.certiflow.ports.InspectionRepository;
import ar.edu.itba.certiflow.usecases.report.ActLine;
import ar.edu.itba.certiflow.usecases.report.GenerateInspectionAct;
import ar.edu.itba.certiflow.usecases.report.InspectionAct;
import java.util.List;

public final class GenerateInspectionActInteractor implements GenerateInspectionAct {

    private final InspectionRepository inspectionRepository;
    private final AssetRepository assetRepository;
    private final AuditLog<Inspection> inspectionLog;

    public GenerateInspectionActInteractor(InspectionRepository inspectionRepository, AssetRepository assetRepository, AuditLog<Inspection> inspectionLog) {
        this.inspectionRepository = inspectionRepository;
        this.assetRepository = assetRepository;
        this.inspectionLog = inspectionLog;
    }

    @Override
    public InspectionAct execute(InspectionId inspectionId) {
        Inspection inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new InspectionNotFoundException(inspectionId));
        inspection.ensureClosed();
        Asset asset = assetRepository.findByCode(inspection.assetCode())
                .orElseThrow(() -> new AssetNotFoundException(inspection.assetCode()));
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
