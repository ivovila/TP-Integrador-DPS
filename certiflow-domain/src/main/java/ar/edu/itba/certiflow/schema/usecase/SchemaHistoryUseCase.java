package ar.edu.itba.certiflow.schema.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface SchemaHistoryUseCase {

    List<AuditEntry> execute(AssetType assetType);
}
