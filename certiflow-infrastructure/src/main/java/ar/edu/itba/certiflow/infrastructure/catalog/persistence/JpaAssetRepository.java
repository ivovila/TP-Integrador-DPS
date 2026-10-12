package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssetRepository implements AssetRepository {

    private final SpringDataAssetRepository assets;

    public JpaAssetRepository(SpringDataAssetRepository assets) {
        this.assets = assets;
    }

    @Override
    public void save(Asset asset) {
        assets.save(AssetEntity.from(asset));
    }

    @Override
    public Optional<Asset> findByCode(AssetCode code) {
        return assets.findById(code.value()).map(AssetEntity::toDomain);
    }

    @Override
    public List<Asset> findAll() {
        return assets.findAll().stream().map(AssetEntity::toDomain).toList();
    }
}
