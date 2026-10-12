package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionSchemaRepository implements InspectionSchemaRepository {

    private final SpringDataInspectionSchemaRepository schemas;

    public JpaInspectionSchemaRepository(SpringDataInspectionSchemaRepository schemas) {
        this.schemas = schemas;
    }

    @Override
    public void save(InspectionSchema schema) {
        schemas.findById(schema.assetType().name()).ifPresentOrElse(
                existing -> existing.appendVersionsOf(schema),
                () -> schemas.save(new InspectionSchemaEntity(schema)));
    }

    @Override
    public Optional<InspectionSchema> findByAssetType(AssetType assetType) {
        return schemas.findById(assetType.name()).map(InspectionSchemaEntity::toDomain);
    }

    public SchemaVersionEntity versionEntity(AssetType assetType, int number) {
        return schemas.findById(assetType.name()).orElseThrow().versions().stream()
                .filter(version -> version.number() == number)
                .findFirst()
                .orElseThrow();
    }
}
