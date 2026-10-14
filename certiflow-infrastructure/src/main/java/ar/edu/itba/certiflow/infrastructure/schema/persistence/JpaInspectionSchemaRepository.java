package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.UnreadableStoredDataException;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.model.exceptions.SchemaAlreadyDefinedForAssetTypeException;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionSchemaRepository implements InspectionSchemaRepository {

    private final SpringDataInspectionSchemaRepository schemas;
    private final SectionsDocumentMapper sectionsDocuments;
    private final CodesDocumentMapper codeDocuments;
    private final EntityManager entityManager;

    public JpaInspectionSchemaRepository(SpringDataInspectionSchemaRepository schemas,
                                         SectionsDocumentMapper sectionsDocuments, CodesDocumentMapper codeDocuments,
                                         EntityManager entityManager) {
        this.schemas = schemas;
        this.sectionsDocuments = sectionsDocuments;
        this.codeDocuments = codeDocuments;
        this.entityManager = entityManager;
    }

    /**
     * Inserta, y lo hace ya. El save de Spring Data sobre una entidad con el id asignado es un merge, que
     * no falla si otro pedido creó primero el esquema de ese tipo de activo; con persist la clave primaria hace
     * fallar al segundo, y ese fallo es la misma regla que el caso de uso ya comprobó antes.
     */
    @Override
    public void add(InspectionSchema schema) {
        try {
            entityManager.persist(new InspectionSchemaEntity(schema, sectionsDocuments, codeDocuments));
            entityManager.flush();
        } catch (ConstraintViolationException assetTypeAlreadyStored) {
            throw new SchemaAlreadyDefinedForAssetTypeException(schema.assetType());
        }
    }

    @Override
    public void save(InspectionSchema schema) {
        schemas.findById(schema.assetType().name())
                .orElseThrow(() -> new UnreadableStoredDataException("inspection schema", schema.assetType().name()))
                .appendVersionsOf(schema, sectionsDocuments, codeDocuments);
    }

    @Override
    public Optional<InspectionSchema> findByAssetType(AssetType assetType) {
        return schemas.findById(assetType.name()).map(this::restore);
    }

    @Override
    public Page<InspectionSchema> findPage(PageRequest request) {
        org.springframework.data.domain.Page<InspectionSchemaEntity> found = schemas.findAll(
                org.springframework.data.domain.PageRequest.of(request.number(), request.size(), Sort.by("assetType")));
        return new Page<>(found.getContent().stream().map(this::restore).toList(), request,
                found.getTotalElements());
    }

    @Override
    public Optional<InspectionSchema> findForUpdate(AssetType assetType) {
        return schemas.findLocked(assetType.name()).map(this::restore);
    }

    private InspectionSchema restore(InspectionSchemaEntity entity) {
        return entity.toDomain(sectionsDocuments);
    }
}
