package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import org.springframework.stereotype.Repository;

/** Bloquea la fila del activo con {@code select ... for update}: se libera cuando termina la transacción. */
@Repository
public class JpaAssetLock implements AssetLock {

    private final SpringDataAssetRepository assets;

    public JpaAssetLock(SpringDataAssetRepository assets) {
        this.assets = assets;
    }

    @Override
    public void lock(AssetCode assetCode) {
        assets.findLocked(assetCode.value()).orElseThrow(() -> new AssetNotFoundException(assetCode));
    }
}
