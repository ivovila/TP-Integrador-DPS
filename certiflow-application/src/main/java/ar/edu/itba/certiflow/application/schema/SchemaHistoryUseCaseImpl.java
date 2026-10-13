package ar.edu.itba.certiflow.application.schema;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.usecase.SchemaHistoryUseCase;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import java.util.List;

public final class SchemaHistoryUseCaseImpl implements SchemaHistoryUseCase {

    private final AuditLog<AssetType> schemaLog;

    public SchemaHistoryUseCaseImpl(AuditLog<AssetType> schemaLog) {
        this.schemaLog = schemaLog;
    }

    @Override
    public List<AuditEntry> execute(AssetType assetType) {
        return schemaLog.historyOf(assetType);
    }
}
