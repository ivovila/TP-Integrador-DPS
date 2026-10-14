package ar.edu.itba.certiflow.infrastructure.catalog.persistence;

import ar.edu.itba.certiflow.catalog.model.Asset;
import ar.edu.itba.certiflow.catalog.model.AssetCode;
import ar.edu.itba.certiflow.catalog.model.exceptions.AssetAlreadyRegisteredException;
import ar.edu.itba.certiflow.catalog.repository.AssetRepository;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.StoredJson;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;

@Repository
public class JpaAssetRepository implements AssetRepository {

    private final SpringDataAssetRepository assets;

    private final StoredJson storedJson;
    private final EntityManager entityManager;

    public JpaAssetRepository(SpringDataAssetRepository assets, StoredJson storedJson, EntityManager entityManager) {
        this.assets = assets;
        this.storedJson = storedJson;
        this.entityManager = entityManager;
    }

    /**
     * Inserta, y lo hace ya. El save de Spring Data sobre una entidad con el id asignado es un merge, que
     * actualiza la fila si otro pedido la creó primero; con persist la clave primaria hace fallar al segundo,
     * y ese fallo es la misma regla que el dominio ya comprobó antes: el código está en uso.
     */
    @Override
    public void add(Asset asset) {
        try {
            entityManager.persist(new AssetEntity(asset, storedJson));
            entityManager.flush();
        } catch (ConstraintViolationException codeAlreadyStored) {
            throw new AssetAlreadyRegisteredException(asset.code());
        }
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
