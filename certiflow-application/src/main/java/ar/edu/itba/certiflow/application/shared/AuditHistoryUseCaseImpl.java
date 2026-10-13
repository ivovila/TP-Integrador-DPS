package ar.edu.itba.certiflow.application.shared;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditLogs;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import java.util.List;

public final class AuditHistoryUseCaseImpl implements AuditHistoryUseCase {

    private final AuditLogs logs;

    public AuditHistoryUseCaseImpl(AuditLog<AssetCode> assetLog, AuditLog<AssetType> schemaLog, AuditLog<InspectionId> inspectionLog,
                                   AuditLog<FindingId> findingLog, AuditLog<CertificateNumber> certificateLog) {
        this.logs = new AuditLogs(assetLog, schemaLog, inspectionLog, findingLog, certificateLog);
    }

    @Override
    public List<AuditEntry> execute(AuditSubject subject) {
        return subject.historyIn(logs);
    }
}
