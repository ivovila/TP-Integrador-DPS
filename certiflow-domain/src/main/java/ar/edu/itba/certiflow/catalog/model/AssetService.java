package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.shared.model.Stamp;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;

/**
 * El alta de un activo es una decisión del negocio: acá vive la unicidad del código y queda su entrada
 * de bitácora. El caso de uso solo busca el activo existente y guarda el nuevo.
 */
public final class AssetService {

    private final AuditLog<AssetCode> assetLog;
    private final AssetRepository assets;

    public AssetService(AuditLog<AssetCode> assetLog, AssetRepository assets) {
        this.assetLog = assetLog;
        this.assets = assets;
    }

    public Asset register(Asset asset, Stamp registered) {
        if (assets.findByCode(asset.code()).isPresent()) {
            throw new AssetAlreadyRegisteredException(asset.code());
        }
        assetLog.record(asset.code(), new AuditEntry(AssetAudit.REGISTERED, registered, asset.name()));
        return asset;
    }
}
