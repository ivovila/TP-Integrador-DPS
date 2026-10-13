package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.inspection.model.CriterionResponse;
import ar.edu.itba.certiflow.inspection.model.Inspection;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.model.exceptions.InspectionNotFoundException;
import ar.edu.itba.certiflow.inspection.repository.InspectionRepository;
import ar.edu.itba.certiflow.inspection.usecase.ActLine;
import ar.edu.itba.certiflow.inspection.usecase.GenerateInspectionActUseCase;
import ar.edu.itba.certiflow.inspection.usecase.InspectionAct;
import ar.edu.itba.certiflow.schema.model.Answer;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class GenerateInspectionActUseCaseImpl implements GenerateInspectionActUseCase {

    private final InspectionRepository inspectionRepository;
    private final AssetRepository assetRepository;
    private final AuditLog<InspectionId> inspectionLog;

    public GenerateInspectionActUseCaseImpl(InspectionRepository inspectionRepository, AssetRepository assetRepository, AuditLog<InspectionId> inspectionLog) {
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
        List<ActLine> actLines = inspection.evaluate().responses().stream()
                .flatMap(response -> linesOf(response).stream())
                .toList();
        return new InspectionAct(asset.code(), asset.name(), inspection.assignment(),
                inspection.schemaVersion().number(), actLines, inspection.revisions(),
                inspectionLog.historyOf(inspection.id()));
    }

    private <A extends Answer> List<ActLine> linesOf(CriterionResponse<A> response) {
        return response.answering().describedBy(new ActLines<>(response));
    }
}
