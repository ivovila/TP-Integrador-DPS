package ar.edu.itba.certiflow.catalog.model;

import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.catalog.model.exceptions.UnknownJurisdictionException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.catalog.repository.KnownJurisdictions;
import ar.edu.itba.certiflow.shared.model.AuditEntry;
import ar.edu.itba.certiflow.shared.model.AuditLog;
import ar.edu.itba.certiflow.shared.model.Stamp;

/**
 * El alta de un activo es una decisión del negocio: acá viven la unicidad del código y la exigencia de una
 * jurisdicción conocida, y queda su entrada de bitácora. El caso de uso solo arma el activo y lo guarda.
 */
public final class AssetService {

    private final AuditLog<AssetCode> assetLog;
    private final AssetRepository assets;
    private final KnownJurisdictions knownJurisdictions;

    public AssetService(AuditLog<AssetCode> assetLog, AssetRepository assets, KnownJurisdictions knownJurisdictions) {
        this.assetLog = assetLog;
        this.assets = assets;
        this.knownJurisdictions = knownJurisdictions;
    }

    public Asset register(Asset asset, Stamp registered) {
        if (!knownJurisdictions.isKnown(asset.jurisdictionCode())) {
            throw new UnknownJurisdictionException(asset.jurisdictionCode());
        }
        if (assets.findByCode(asset.code()).isPresent()) {
            throw new AssetAlreadyRegisteredException(asset.code());
        }
        assetLog.record(asset.code(), new AuditEntry(AssetAudit.REGISTERED, registered, asset.name()));
        return asset;
    }
}
