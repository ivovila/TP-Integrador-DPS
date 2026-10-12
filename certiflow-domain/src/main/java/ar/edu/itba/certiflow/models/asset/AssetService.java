package ar.edu.itba.certiflow.models.asset;

import ar.edu.itba.certiflow.models.asset.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.models.audit.AuditEntry;
import ar.edu.itba.certiflow.models.audit.AuditLog;
import ar.edu.itba.certiflow.models.shared.Person;
import java.time.Instant;
import java.util.Optional;

/**
 * El alta de un activo es una decisión del negocio: acá vive la unicidad del código y queda su entrada
 * de bitácora. El caso de uso solo busca el activo existente y guarda el nuevo.
 */
public final class AssetService {

    private final AuditLog<AssetCode> assetLog;

    public AssetService(AuditLog<AssetCode> assetLog) {
        this.assetLog = assetLog;
    }

    public Asset register(Asset asset, Optional<Asset> alreadyRegistered, Person registeredBy, Instant registeredAt) {
        alreadyRegistered.ifPresent(existingAsset -> {
            throw new AssetAlreadyRegisteredException(existingAsset.code());
        });
        assetLog.record(asset.code(), new AuditEntry(AssetAudit.REGISTERED, registeredBy, registeredAt, asset.name()));
        return asset;
    }
}
