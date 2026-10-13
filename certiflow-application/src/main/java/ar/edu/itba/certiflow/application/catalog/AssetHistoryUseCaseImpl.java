package ar.edu.itba.certiflow.application.catalog;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.usecase.AssetHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class AssetHistoryUseCaseImpl implements AssetHistoryUseCase {

    private final AuditLog<AssetCode> assetLog;

    public AssetHistoryUseCaseImpl(AuditLog<AssetCode> assetLog) {
        this.assetLog = assetLog;
    }

    @Override
    public List<AuditEntry> execute(AssetCode assetCode) {
        return assetLog.historyOf(assetCode);
    }
}
