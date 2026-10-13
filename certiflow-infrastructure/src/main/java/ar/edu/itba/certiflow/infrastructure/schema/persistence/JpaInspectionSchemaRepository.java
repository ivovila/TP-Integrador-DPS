package ar.edu.itba.certiflow.infrastructure.schema.persistence;

import ar.edu.itba.certiflow.catalog.model.AssetType;
import ar.edu.itba.certiflow.infrastructure.shared.persistence.CodesDocumentMapper;
import ar.edu.itba.certiflow.schema.model.InspectionSchema;
import ar.edu.itba.certiflow.schema.repository.InspectionSchemaRepository;
import ar.edu.itba.certiflow.shared.model.Page;
import ar.edu.itba.certiflow.shared.model.PageRequest;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaInspectionSchemaRepository implements InspectionSchemaRepository {

    private final SpringDataInspectionSchemaRepository schemas;
    private final SectionsDocumentMapper sectionsDocuments;
    private final CodesDocumentMapper codeDocuments;

    public JpaInspectionSchemaRepository(SpringDataInspectionSchemaRepository schemas,
                                         SectionsDocumentMapper sectionsDocuments, CodesDocumentMapper codeDocuments) {
        this.schemas = schemas;
        this.sectionsDocuments = sectionsDocuments;
        this.codeDocuments = codeDocuments;
    }

    @Override
    public void save(InspectionSchema schema) {
        schemas.findById(schema.assetType().name()).ifPresentOrElse(
                existing -> existing.appendVersionsOf(schema, sectionsDocuments, codeDocuments),
                () -> schemas.save(new InspectionSchemaEntity(schema, sectionsDocuments, codeDocuments)));
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
