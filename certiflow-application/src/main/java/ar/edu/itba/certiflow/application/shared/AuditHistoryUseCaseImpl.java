package ar.edu.itba.certiflow.application.shared;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.certification.model.CertificateNumber;
import ar.edu.itba.certiflow.inspection.model.FindingId;
import ar.edu.itba.certiflow.inspection.model.InspectionId;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.usecase.AuditHistoryUseCase;
import ar.edu.itba.certiflow.shared.usecase.AuditSubject;
import java.util.List;

public final class AuditHistoryUseCaseImpl implements AuditHistoryUseCase {

    private final AuditLog<AssetCode> assetLog;
    private final AuditLog<AssetType> schemaLog;
    private final AuditLog<InspectionId> inspectionLog;
    private final AuditLog<FindingId> findingLog;
    private final AuditLog<CertificateNumber> certificateLog;

    public AuditHistoryUseCaseImpl(AuditLog<AssetCode> assetLog, AuditLog<AssetType> schemaLog, AuditLog<InspectionId> inspectionLog,
                                   AuditLog<FindingId> findingLog, AuditLog<CertificateNumber> certificateLog) {
        this.assetLog = assetLog;
        this.schemaLog = schemaLog;
        this.inspectionLog = inspectionLog;
        this.findingLog = findingLog;
        this.certificateLog = certificateLog;
    }

    @Override
    public List<AuditEntry> execute(AuditSubject subject) {
        return switch (subject) {
            case AuditSubject.OfAsset(AssetCode code) -> assetLog.historyOf(code);
            case AuditSubject.OfSchema(AssetType type) -> schemaLog.historyOf(type);
            case AuditSubject.OfInspection(InspectionId id) -> inspectionLog.historyOf(id);
            case AuditSubject.OfFinding(FindingId id) -> findingLog.historyOf(id);
            case AuditSubject.OfCertificate(CertificateNumber number) -> certificateLog.historyOf(number);
        };
    }
}
