package ar.edu.itba.certiflow.application.inspection;

import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.inspection.usecase.InspectionHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class InspectionHistoryUseCaseImpl implements InspectionHistoryUseCase {

    private final AuditLog<InspectionId> inspectionLog;

    public InspectionHistoryUseCaseImpl(AuditLog<InspectionId> inspectionLog) {
        this.inspectionLog = inspectionLog;
    }

    @Override
    public List<AuditEntry> execute(InspectionId inspectionId) {
        return inspectionLog.historyOf(inspectionId);
    }
}
