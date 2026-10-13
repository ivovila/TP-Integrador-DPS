package ar.edu.itba.certiflow.catalog.usecase;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import java.util.List;

public interface AssetHistoryUseCase {

    List<AuditEntry> execute(AssetCode assetCode);
}
