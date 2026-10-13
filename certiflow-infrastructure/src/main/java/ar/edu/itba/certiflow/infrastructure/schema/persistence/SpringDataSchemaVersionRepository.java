package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataSchemaVersionRepository extends JpaRepository<SchemaVersionEntity, Long> {

    Optional<SchemaVersionEntity> findByAssetTypeAndNumber(String assetType, int number);
}
