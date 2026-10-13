package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssetRepository implements AssetRepository {

    private final SpringDataAssetRepository assets;

    private final StoredJson storedJson;

    public JpaAssetRepository(SpringDataAssetRepository assets, StoredJson storedJson) {
        this.assets = assets;
        this.storedJson = storedJson;
    }

    @Override
    public void save(Asset asset) {
        assets.save(new AssetEntity(asset, storedJson));
    }

    @Override
    public Optional<Asset> findByCode(AssetCode code) {
        return assets.findById(code.value()).map(entity -> entity.toDomain(storedJson));
    }

    @Override
    public List<Asset> findAll() {
        return assets.findAll().stream().map(entity -> entity.toDomain(storedJson)).toList();
    }
}
