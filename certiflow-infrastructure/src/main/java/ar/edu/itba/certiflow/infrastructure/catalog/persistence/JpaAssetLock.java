package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetNotFoundException;
import ar.edu.itba.certiflow.catalog.repository.AssetLock;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssetLock implements AssetLock {
    private final SpringDataAssetRepository assets;
    public JpaAssetLock(SpringDataAssetRepository assets) { this.assets = assets; }
    @Override public void lock(AssetCode code) { assets.findLocked(code.value()).orElseThrow(() -> new AssetNotFoundException(code)); }
}
